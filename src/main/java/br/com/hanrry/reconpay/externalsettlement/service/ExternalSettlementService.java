package br.com.hanrry.reconpay.externalsettlement.service;

import br.com.hanrry.reconpay.exception.DuplicateExternalSettlementException;
import br.com.hanrry.reconpay.exception.ExternalSettlementNotFoundException;
import br.com.hanrry.reconpay.exception.InvalidSettlementImportException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.SettlementImportNotFoundException;
import br.com.hanrry.reconpay.externalsettlement.dto.ExternalSettlementResponseDTO;
import br.com.hanrry.reconpay.externalsettlement.dto.SettlementImportResponseDTO;
import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.externalsettlement.entity.SettlementImportEntity;
import br.com.hanrry.reconpay.externalsettlement.mapper.IExternalSettlementMapper;
import br.com.hanrry.reconpay.externalsettlement.mapper.ISettlementImportMapper;
import br.com.hanrry.reconpay.externalsettlement.repository.ExternalSettlementSpecifications;
import br.com.hanrry.reconpay.externalsettlement.repository.IExternalSettlementRepository;
import br.com.hanrry.reconpay.externalsettlement.repository.ISettlementImportRepository;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExternalSettlementService {

    private final SettlementCsvParser settlementCsvParser;
    private final IExternalSettlementMapper externalSettlementMapper;
    private final ISettlementImportMapper settlementImportMapper;
    private final IExternalSettlementRepository externalSettlementRepository;
    private final ISettlementImportRepository settlementImportRepository;
    private final IMerchantRepository merchantRepository;
    private final AuditLogger auditLogger;

    @Transactional
    public SettlementImportResponseDTO importCsv(UUID merchantId, MultipartFile file) {
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));

        validateFile(file);

        List<SettlementCsvParser.ParsedSettlementRow> rows = parseFile(file);
        ensureNoDuplicateInDatabase(merchantId, rows);

        SettlementImportEntity importBatch = new SettlementImportEntity();
        importBatch.setMerchant(merchant);
        importBatch.setFileName(file.getOriginalFilename());
        importBatch.setTotalRows(rows.size());

        SettlementImportEntity savedImport = settlementImportRepository.save(importBatch);

        List<ExternalSettlementEntity> settlements = rows.stream()
                .map(row -> toEntity(merchant, savedImport, row))
                .toList();

        externalSettlementRepository.saveAll(settlements);
        auditLogger.record("SETTLEMENTS_IMPORTED", "settlementImport", savedImport.getId(),
                "merchant=" + merchantId + " rows=" + rows.size());

        return settlementImportMapper.toDTO(savedImport);
    }

    @Transactional(readOnly = true)
    public Page<SettlementImportResponseDTO> findAllImports(UUID merchantId, Pageable pageable) {
        ensureMerchantExists(merchantId);

        return settlementImportRepository.findAllByMerchant_Id(merchantId, pageable)
                .map(settlementImportMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public SettlementImportResponseDTO findImportById(UUID merchantId, UUID importId) {
        ensureMerchantExists(merchantId);

        SettlementImportEntity importBatch = settlementImportRepository.findByIdAndMerchant_Id(importId, merchantId)
                .orElseThrow(() -> new SettlementImportNotFoundException(
                        "Importação não encontrada com id: " + importId));

        return settlementImportMapper.toDTO(importBatch);
    }

    @Transactional(readOnly = true)
    public Page<ExternalSettlementResponseDTO> findAll(
            UUID merchantId,
            TransactionStatus status,
            PaymentMethod paymentMethod,
            LocalDate fromDate,
            LocalDate toDate,
            UUID importId,
            Pageable pageable) {
        ensureMerchantExists(merchantId);

        return externalSettlementRepository.findAll(
                        ExternalSettlementSpecifications.withFilters(
                                merchantId, status, paymentMethod, fromDate, toDate, importId),
                        pageable)
                .map(externalSettlementMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public ExternalSettlementResponseDTO findById(UUID merchantId, UUID id) {
        ExternalSettlementEntity settlement = findSettlementForMerchant(merchantId, id);
        return externalSettlementMapper.toDTO(settlement);
    }

    private ExternalSettlementEntity findSettlementForMerchant(UUID merchantId, UUID id) {
        ensureMerchantExists(merchantId);

        return externalSettlementRepository.findByIdAndMerchant_Id(id, merchantId)
                .orElseThrow(() -> new ExternalSettlementNotFoundException(
                        "Liquidação externa não encontrada com id: " + id));
    }

    private void ensureMerchantExists(UUID merchantId) {
        merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidSettlementImportException("Arquivo CSV é obrigatório");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            throw new InvalidSettlementImportException("Arquivo deve ser um CSV (.csv)");
        }
    }

    private List<SettlementCsvParser.ParsedSettlementRow> parseFile(MultipartFile file) {
        try {
            return settlementCsvParser.parse(file.getInputStream());
        } catch (IOException ex) {
            throw new InvalidSettlementImportException("Erro ao ler arquivo CSV");
        }
    }

    private void ensureNoDuplicateInDatabase(
            UUID merchantId,
            List<SettlementCsvParser.ParsedSettlementRow> rows) {
        List<String> externalReferences = rows.stream()
                .map(SettlementCsvParser.ParsedSettlementRow::externalReference)
                .toList();

        List<String> conflictingReferences = externalSettlementRepository
                .findByMerchant_IdAndExternalReferenceIn(merchantId, externalReferences)
                .stream()
                .map(ExternalSettlementEntity::getExternalReference)
                .toList();

        if (!conflictingReferences.isEmpty()) {
            throw new DuplicateExternalSettlementException(
                    "Referências externas já importadas para este comerciante: "
                            + String.join(", ", conflictingReferences),
                    conflictingReferences);
        }
    }

    private ExternalSettlementEntity toEntity(
            MerchantEntity merchant,
            SettlementImportEntity importBatch,
            SettlementCsvParser.ParsedSettlementRow row) {
        ExternalSettlementEntity entity = new ExternalSettlementEntity();
        entity.setMerchant(merchant);
        entity.setImportBatch(importBatch);
        entity.setExternalReference(row.externalReference());
        entity.setAmount(row.amount());
        entity.setNetAmount(row.netAmount());
        entity.setPaymentMethod(row.paymentMethod());
        entity.setInstallments(row.installments());
        entity.setStatus(row.status());
        entity.setSettlementDate(row.settlementDate());
        return entity;
    }
}
