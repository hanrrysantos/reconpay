package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.ReconciliationNotFoundException;
import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.externalsettlement.repository.ExternalSettlementSpecifications;
import br.com.hanrry.reconpay.externalsettlement.repository.IExternalSettlementRepository;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    @Transactional
    public ReconciliationRunResponseDTO run(UUID merchantId, RunReconciliationRequestDTO request) {
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));

        LocalDate fromDate = request.fromDate();
        LocalDate toDate = request.toDate();

        Map<String, InternalTransactionEntity> transactionsByReference = transactionRepository.findAll(
                        TransactionSpecifications.withFilters(merchantId, null, null, fromDate, toDate))
                .stream()
                .collect(Collectors.toMap(
                        InternalTransactionEntity::getExternalReference,
                        Function.identity()));

        Map<String, ExternalSettlementEntity> settlementsByReference = externalSettlementRepository.findAll(
                        ExternalSettlementSpecifications.withDateRange(merchantId, fromDate, toDate))
                .stream()
                .collect(Collectors.toMap(
                        ExternalSettlementEntity::getExternalReference,
                        Function.identity()));

        List<ReconciliationItemEntity> items = reconciliationEngine.reconcile(
                transactionsByReference,
                settlementsByReference);

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

        items.forEach(item -> item.setReconciliationRun(savedRun));
        reconciliationItemRepository.saveAll(items);

        return reconciliationMapper.toRunDTO(savedRun);
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

    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID merchantId, UUID runId) {
        findRunForMerchant(merchantId, runId);

        List<ReconciliationItemEntity> items = reconciliationItemRepository
                .findAllWithDetailsByRunId(runId);

        return reconciliationCsvExporter.export(items);
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
