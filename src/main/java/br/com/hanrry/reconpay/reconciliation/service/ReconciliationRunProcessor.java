package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.exception.ReconciliationNotFoundException;
import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.externalsettlement.repository.ExternalSettlementSpecifications;
import br.com.hanrry.reconpay.externalsettlement.repository.IExternalSettlementRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationItemRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import br.com.hanrry.reconpay.transaction.repository.IInternalTransactionRepository;
import br.com.hanrry.reconpay.transaction.repository.TransactionSpecifications;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Runs the reconciliation for an already persisted run. Kept apart from
 * {@link ReconciliationService} so the worker's transaction boundaries are the
 * proxy's, not a self-invocation that would silently run without one.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationRunProcessor {

    private static final int PERSIST_BATCH_SIZE = 500;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private final ReconciliationEngine reconciliationEngine;
    private final IReconciliationRunRepository reconciliationRunRepository;
    private final IReconciliationItemRepository reconciliationItemRepository;
    private final IInternalTransactionRepository transactionRepository;
    private final IExternalSettlementRepository externalSettlementRepository;
    private final ReconciliationProperties properties;
    private final AuditLogger auditLogger;
    private final EntityManager entityManager;
    private final Clock clock;

    /*
     * REQUIRES_NEW because the AFTER_COMMIT callback still runs inside the
     * requesting transaction's synchronization scope. A plain REQUIRED would
     * join that already committed transaction and every write would fail with
     * "no transaction is in progress".
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(UUID runId) {
        ReconciliationRunEntity run = reconciliationRunRepository.findById(runId)
                .orElseThrow(() -> new ReconciliationNotFoundException(
                        "Conciliação não encontrada com id: " + runId));

        UUID merchantId = run.getMerchant().getId();
        LocalDate fromDate = run.getFromDate();
        LocalDate toDate = run.getToDate();

        run.setStatus(ReconciliationRunStatus.RUNNING);
        run.setStartedAt(Instant.now(clock));
        reconciliationRunRepository.saveAndFlush(run);

        Map<String, InternalTransactionEntity> transactionsByReference = transactionRepository.findAll(
                        TransactionSpecifications.withFilters(merchantId, null, null, fromDate, toDate))
                .stream()
                .collect(Collectors.toMap(
                        InternalTransactionEntity::getExternalReference,
                        Function.identity()));

        Map<String, ExternalSettlementEntity> settlementsByReference = loadSettlementsInScope(
                merchantId, fromDate, toDate, transactionsByReference.keySet());

        List<ReconciliationItemEntity> items = reconciliationEngine.reconcile(
                transactionsByReference,
                settlementsByReference);

        persistInBatches(items, runId);

        reconciliationRunRepository.supersedeWindow(
                merchantId, fromDate, toDate, runId, Instant.now(clock));

        ReconciliationRunEntity finished = reconciliationRunRepository.findById(runId)
                .orElseThrow(() -> new ReconciliationNotFoundException(
                        "Conciliação não encontrada com id: " + runId));
        finished.setTotalItems(items.size());
        finished.setMatchedCount(countByResult(items, ReconciliationResult.MATCHED));
        finished.setDivergentCount(countByResult(items, ReconciliationResult.DIVERGENT));
        finished.setStatus(ReconciliationRunStatus.COMPLETED);
        finished.setFinishedAt(Instant.now(clock));
        reconciliationRunRepository.save(finished);

        auditLogger.record("RECONCILIATION_COMPLETED", "reconciliationRun", runId,
                "merchant=" + merchantId + " window=" + fromDate + ".." + toDate
                        + " total=" + items.size()
                        + " divergent=" + finished.getDivergentCount());
    }

    /*
     * Runs in its own transaction because the one that failed is already marked
     * rollback-only, and a run stuck at RUNNING would block the window forever.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID runId, String reason) {
        reconciliationRunRepository.findById(runId).ifPresent(run -> {
            run.setStatus(ReconciliationRunStatus.FAILED);
            run.setFinishedAt(Instant.now(clock));
            run.setErrorMessage(truncate(reason));
            reconciliationRunRepository.save(run);
            auditLogger.record("RECONCILIATION_FAILED", "reconciliationRun", runId, "reason=" + reason);
        });
    }

    /*
     * Settlements lag their transaction, so the settlement side reads a window
     * extended by settlementLagDays. That extension also pulls in payouts for
     * transactions dated after toDate, which are out of scope rather than
     * orphans, so anything whose reference exists elsewhere for the merchant is
     * dropped instead of being reported twice across adjacent runs.
     */
    private Map<String, ExternalSettlementEntity> loadSettlementsInScope(
            UUID merchantId,
            LocalDate fromDate,
            LocalDate toDate,
            Set<String> referencesInWindow) {
        List<ExternalSettlementEntity> settlements = externalSettlementRepository.findAll(
                ExternalSettlementSpecifications.withDateRange(
                        merchantId, fromDate, toDate.plusDays(properties.settlementLagDays())));

        Set<String> unmatchedReferences = settlements.stream()
                .map(ExternalSettlementEntity::getExternalReference)
                .filter(reference -> !referencesInWindow.contains(reference))
                .collect(Collectors.toSet());

        Set<String> knownOutsideWindow = unmatchedReferences.isEmpty()
                ? Set.of()
                : transactionRepository.findExistingReferences(merchantId, unmatchedReferences);

        return settlements.stream()
                .filter(settlement -> !knownOutsideWindow.contains(settlement.getExternalReference()))
                .collect(Collectors.toMap(
                        ExternalSettlementEntity::getExternalReference,
                        Function.identity()));
    }

    /*
     * Clearing the persistence context between batches keeps Hibernate's dirty
     * checking from degrading as the item count grows, at the cost of having to
     * re-attach the run reference on every batch.
     */
    private void persistInBatches(List<ReconciliationItemEntity> items, UUID runId) {
        for (int start = 0; start < items.size(); start += PERSIST_BATCH_SIZE) {
            int end = Math.min(start + PERSIST_BATCH_SIZE, items.size());
            List<ReconciliationItemEntity> batch = items.subList(start, end);

            ReconciliationRunEntity runReference =
                    entityManager.getReference(ReconciliationRunEntity.class, runId);
            batch.forEach(item -> item.setReconciliationRun(runReference));

            reconciliationItemRepository.saveAll(batch);
            entityManager.flush();
            entityManager.clear();
        }
    }

    private int countByResult(List<ReconciliationItemEntity> items, ReconciliationResult result) {
        return (int) items.stream()
                .filter(item -> item.getResult() == result)
                .count();
    }

    private String truncate(String reason) {
        if (reason == null) {
            return "Erro desconhecido";
        }
        return reason.length() <= MAX_ERROR_MESSAGE_LENGTH
                ? reason
                : reason.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
