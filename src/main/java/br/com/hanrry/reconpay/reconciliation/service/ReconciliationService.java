package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.exception.InvalidReconciliationWindowException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.ReconciliationNotFoundException;
import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.externalsettlement.repository.ExternalSettlementSpecifications;
import br.com.hanrry.reconpay.externalsettlement.repository.IExternalSettlementRepository;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.reconciliation.dto.ReconciliationItemResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.ReconciliationRunResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.RunReconciliationRequestDTO;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import br.com.hanrry.reconpay.reconciliation.mapper.IReconciliationMapper;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationItemRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.reconciliation.repository.ReconciliationItemSpecifications;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import br.com.hanrry.reconpay.transaction.repository.IInternalTransactionRepository;
import br.com.hanrry.reconpay.transaction.repository.TransactionSpecifications;
import com.opencsv.CSVWriter;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final ReconciliationEngine reconciliationEngine;
    private final ReconciliationCsvExporter reconciliationCsvExporter;
    private final IReconciliationMapper reconciliationMapper;
    private final IReconciliationRunRepository reconciliationRunRepository;
    private final IReconciliationItemRepository reconciliationItemRepository;
    private final IInternalTransactionRepository transactionRepository;
    private final IExternalSettlementRepository externalSettlementRepository;
    private final IMerchantRepository merchantRepository;
    private final ReconciliationProperties properties;
    private final AuditLogger auditLogger;
    private final EntityManager entityManager;
    private final Clock clock;

    private static final int PERSIST_BATCH_SIZE = 500;
    private static final int EXPORT_CHUNK_SIZE = 500;

    @Transactional
    public ReconciliationRunResponseDTO run(UUID merchantId, RunReconciliationRequestDTO request) {
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));

        LocalDate fromDate = request.fromDate();
        LocalDate toDate = request.toDate();
        ensureWindowIsWithinLimit(fromDate, toDate);

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

        reconciliationRunRepository.supersedeWindow(merchantId, fromDate, toDate, Instant.now(clock));

        ReconciliationRunEntity run = new ReconciliationRunEntity();
        run.setMerchant(merchant);
        run.setFromDate(fromDate);
        run.setToDate(toDate);
        run.setTotalItems(items.size());
        run.setMatchedCount((int) items.stream()
                .filter(item -> item.getResult() == ReconciliationResult.MATCHED)
                .count());
        run.setDivergentCount((int) items.stream()
                .filter(item -> item.getResult() == ReconciliationResult.DIVERGENT)
                .count());

        ReconciliationRunEntity savedRun = reconciliationRunRepository.save(run);
        ReconciliationRunResponseDTO response = reconciliationMapper.toRunDTO(savedRun);
        auditLogger.record("RECONCILIATION_RUN", "reconciliationRun", savedRun.getId(),
                "merchant=" + merchantId + " window=" + fromDate + ".." + toDate
                        + " divergent=" + run.getDivergentCount());

        persistInBatches(items, savedRun.getId());

        return response;
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

    private void ensureWindowIsWithinLimit(LocalDate fromDate, LocalDate toDate) {
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (days > properties.maxWindowDays()) {
            throw new InvalidReconciliationWindowException(
                    "Janela de conciliação excede o máximo de " + properties.maxWindowDays() + " dias");
        }
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

    @Transactional(readOnly = true)
    public Page<ReconciliationRunResponseDTO> findAllRuns(UUID merchantId, Pageable pageable) {
        ensureMerchantExists(merchantId);

        return reconciliationRunRepository.findAllByMerchant_Id(merchantId, pageable)
                .map(reconciliationMapper::toRunDTO);
    }

    @Transactional(readOnly = true)
    public ReconciliationRunResponseDTO findRunById(UUID merchantId, UUID runId) {
        ReconciliationRunEntity run = findRunForMerchant(merchantId, runId);
        return reconciliationMapper.toRunDTO(run);
    }

    @Transactional(readOnly = true)
    public Page<ReconciliationItemResponseDTO> findItems(
            UUID merchantId,
            UUID runId,
            ReconciliationResult result,
            DiscrepancyType discrepancyType,
            Pageable pageable) {
        findRunForMerchant(merchantId, runId);

        return reconciliationItemRepository.findAll(
                        ReconciliationItemSpecifications.withFilters(runId, result, discrepancyType),
                        pageable)
                .map(reconciliationMapper::toItemDTO);
    }

    /*
     * Written straight to the response instead of buffering the whole report,
     * reading items in chunks and clearing the persistence context between them
     * so peak memory does not scale with the size of the run.
     */
    @Transactional(readOnly = true)
    public void exportCsv(UUID merchantId, UUID runId, OutputStream outputStream) {
        findRunForMerchant(merchantId, runId);

        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CSVWriter csvWriter = reconciliationCsvExporter.open(writer)) {

            Pageable chunk = PageRequest.of(0, EXPORT_CHUNK_SIZE);
            Slice<UUID> ids;

            do {
                ids = reconciliationItemRepository.findIdsByRunId(runId, chunk);

                if (!ids.isEmpty()) {
                    reconciliationCsvExporter.write(
                            csvWriter,
                            reconciliationItemRepository.findAllWithDiscrepanciesByIdIn(ids.getContent()));
                    entityManager.clear();
                }

                chunk = chunk.next();
            } while (ids.hasNext());
        } catch (IOException ex) {
            throw new IllegalStateException("Erro ao gerar relatório CSV", ex);
        }
    }

    private ReconciliationRunEntity findRunForMerchant(UUID merchantId, UUID runId) {
        ensureMerchantExists(merchantId);

        return reconciliationRunRepository.findByIdAndMerchant_Id(runId, merchantId)
                .orElseThrow(() -> new ReconciliationNotFoundException(
                        "Conciliação não encontrada com id: " + runId));
    }

    private void ensureMerchantExists(UUID merchantId) {
        merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));
    }
}
