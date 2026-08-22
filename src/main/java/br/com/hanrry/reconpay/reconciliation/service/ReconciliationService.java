package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.exception.InvalidReconciliationWindowException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.ReconciliationNotFoundException;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.reconciliation.dto.ReconciliationItemResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.ReconciliationRunResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.RunReconciliationRequestDTO;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.event.ReconciliationRunRequestedEvent;
import br.com.hanrry.reconpay.reconciliation.mapper.IReconciliationMapper;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationItemRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.reconciliation.repository.ReconciliationItemSpecifications;
import com.opencsv.CSVWriter;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final ReconciliationCsvExporter reconciliationCsvExporter;
    private final IReconciliationMapper reconciliationMapper;
    private final IReconciliationRunRepository reconciliationRunRepository;
    private final IReconciliationItemRepository reconciliationItemRepository;
    private final IMerchantRepository merchantRepository;
    private final ReconciliationProperties properties;
    private final AuditLogger auditLogger;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    private static final int EXPORT_CHUNK_SIZE = 500;

    /*
     * A full window can take minutes, which is well past what a client should
     * hold a connection open for. The request only records the intent and the
     * work is picked up after commit; the caller polls the run for its outcome.
     */
    @Transactional
    public ReconciliationRunResponseDTO run(UUID merchantId, RunReconciliationRequestDTO request) {
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));

        LocalDate fromDate = request.fromDate();
        LocalDate toDate = request.toDate();
        ensureWindowIsWithinLimit(fromDate, toDate);

        ReconciliationRunEntity run = new ReconciliationRunEntity();
        run.setMerchant(merchant);
        run.setFromDate(fromDate);
        run.setToDate(toDate);
        run.setStatus(ReconciliationRunStatus.PENDING);
        run.setTotalItems(0);
        run.setMatchedCount(0);
        run.setDivergentCount(0);

        // Flushed here so the in-flight unique index rejects a concurrent request
        // for the same window as a 409 instead of failing at commit.
        ReconciliationRunEntity savedRun = reconciliationRunRepository.saveAndFlush(run);

        auditLogger.record("RECONCILIATION_REQUESTED", "reconciliationRun", savedRun.getId(),
                "merchant=" + merchantId + " window=" + fromDate + ".." + toDate);

        eventPublisher.publishEvent(new ReconciliationRunRequestedEvent(savedRun.getId()));

        return reconciliationMapper.toRunDTO(savedRun);
    }

    private void ensureWindowIsWithinLimit(LocalDate fromDate, LocalDate toDate) {
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (days > properties.maxWindowDays()) {
            throw new InvalidReconciliationWindowException(
                    "Janela de conciliação excede o máximo de " + properties.maxWindowDays() + " dias");
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
