package br.com.hanrry.reconpay.bankstatement.service;

import br.com.hanrry.reconpay.bankstatement.dto.BankStatementImportResponseDTO;
import br.com.hanrry.reconpay.bankstatement.dto.BankStatementLineResponseDTO;
import br.com.hanrry.reconpay.bankstatement.entity.BankStatementImportEntity;
import br.com.hanrry.reconpay.bankstatement.entity.BankStatementLineEntity;
import br.com.hanrry.reconpay.bankstatement.mapper.IBankStatementImportMapper;
import br.com.hanrry.reconpay.bankstatement.mapper.IBankStatementLineMapper;
import br.com.hanrry.reconpay.bankstatement.repository.IBankStatementImportRepository;
import br.com.hanrry.reconpay.bankstatement.repository.IBankStatementLineRepository;
import br.com.hanrry.reconpay.exception.DuplicateExternalSettlementException;
import br.com.hanrry.reconpay.exception.InvalidSettlementImportException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankStatementService {

    private final BankStatementCsvParser bankStatementCsvParser;
    private final IBankStatementLineMapper bankStatementLineMapper;
    private final IBankStatementImportMapper bankStatementImportMapper;
    private final IBankStatementLineRepository bankStatementLineRepository;
    private final IBankStatementImportRepository bankStatementImportRepository;
    private final IMerchantRepository merchantRepository;
    private final AuditLogger auditLogger;

    @Transactional
    public BankStatementImportResponseDTO importCsv(UUID merchantId, MultipartFile file) {
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));

        validateFile(file);

        List<BankStatementCsvParser.ParsedBankStatementRow> rows = parseFile(file);
        ensureNoDuplicateInDatabase(merchantId, rows);

        BankStatementImportEntity importBatch = new BankStatementImportEntity();
        importBatch.setMerchant(merchant);
        importBatch.setFileName(file.getOriginalFilename());
        importBatch.setTotalRows(rows.size());

        BankStatementImportEntity savedImport = bankStatementImportRepository.save(importBatch);

        List<BankStatementLineEntity> lines = rows.stream()
                .map(row -> toEntity(merchant, savedImport, row))
                .toList();

        bankStatementLineRepository.saveAll(lines);
        auditLogger.record("BANK_STATEMENTS_IMPORTED", "bankStatementImport", savedImport.getId(),
                "merchant=" + merchantId + " rows=" + rows.size());

        return bankStatementImportMapper.toDTO(savedImport);
    }

    @Transactional(readOnly = true)
    public Page<BankStatementLineResponseDTO> findAll(UUID merchantId, UUID importId, Pageable pageable) {
        ensureMerchantExists(merchantId);

        Page<BankStatementLineEntity> lines = importId == null
                ? bankStatementLineRepository.findByMerchant_Id(merchantId, pageable)
                : bankStatementLineRepository.findByMerchant_IdAndImportBatch_Id(
                        merchantId, importId, pageable);

        return lines.map(bankStatementLineMapper::toDTO);
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

    private List<BankStatementCsvParser.ParsedBankStatementRow> parseFile(MultipartFile file) {
        try {
            return bankStatementCsvParser.parse(file.getInputStream());
        } catch (IOException ex) {
            throw new InvalidSettlementImportException("Erro ao ler arquivo CSV");
        }
    }

    private void ensureNoDuplicateInDatabase(
            UUID merchantId,
            List<BankStatementCsvParser.ParsedBankStatementRow> rows) {
        List<String> lineReferences = rows.stream()
                .map(BankStatementCsvParser.ParsedBankStatementRow::lineReference)
                .toList();

        List<String> conflictingReferences = bankStatementLineRepository
                .findByMerchant_IdAndLineReferenceIn(merchantId, lineReferences)
                .stream()
                .map(BankStatementLineEntity::getLineReference)
                .toList();

        if (!conflictingReferences.isEmpty()) {
            throw new DuplicateExternalSettlementException(
                    "Referências de linha já importadas para este comerciante: "
                            + String.join(", ", conflictingReferences),
                    conflictingReferences);
        }
    }

    private BankStatementLineEntity toEntity(
            MerchantEntity merchant,
            BankStatementImportEntity importBatch,
            BankStatementCsvParser.ParsedBankStatementRow row) {
        BankStatementLineEntity entity = new BankStatementLineEntity();
        entity.setMerchant(merchant);
        entity.setImportBatch(importBatch);
        entity.setLineReference(row.lineReference());
        entity.setExternalReference(row.externalReference());
        entity.setAmount(row.amount());
        entity.setMovementDate(row.movementDate());
        return entity;
    }
}
