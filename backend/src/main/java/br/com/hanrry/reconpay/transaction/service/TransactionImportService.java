package br.com.hanrry.reconpay.transaction.service;

import br.com.hanrry.reconpay.exception.DuplicateTransactionReferenceException;
import br.com.hanrry.reconpay.exception.InvalidTransactionAmountException;
import br.com.hanrry.reconpay.exception.InvalidTransactionImportException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.TransactionImportNotFoundException;
import br.com.hanrry.reconpay.exception.TransactionImportValidationException;
import br.com.hanrry.reconpay.externalsettlement.dto.ImportRowErrorDTO;
import br.com.hanrry.reconpay.feerule.entity.FeeRuleEntity;
import br.com.hanrry.reconpay.feerule.repository.IFeeRuleRepository;
import br.com.hanrry.reconpay.feerule.service.ExpectedNetAmountCalculator;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.reconciliation.service.PeriodGuard;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.dto.TransactionImportResponseDTO;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import br.com.hanrry.reconpay.transaction.entity.TransactionImportEntity;
import br.com.hanrry.reconpay.transaction.mapper.ITransactionImportMapper;
import br.com.hanrry.reconpay.transaction.repository.IInternalTransactionRepository;
import br.com.hanrry.reconpay.transaction.repository.ITransactionImportRepository;
import br.com.hanrry.reconpay.transaction.service.TransactionCsvParser.ParseResult;
import br.com.hanrry.reconpay.transaction.service.TransactionCsvParser.ParsedTransactionRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/*
 * All-or-nothing CSV import of internal transactions: every row error (format, fee rule, expected
 * net) comes back at once, then references already stored and locked periods; nothing is saved
 * unless the whole file passes.
 */
@Service
@RequiredArgsConstructor
public class TransactionImportService {

    private final TransactionCsvParser transactionCsvParser;
    private final IMerchantRepository merchantRepository;
    private final IFeeRuleRepository feeRuleRepository;
    private final IInternalTransactionRepository transactionRepository;
    private final ITransactionImportRepository transactionImportRepository;
    private final ITransactionImportMapper transactionImportMapper;
    private final PeriodGuard periodGuard;
    private final AuditLogger auditLogger;

    @Transactional
    public TransactionImportResponseDTO importCsv(UUID merchantId, MultipartFile file) {
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));

        validateFile(file);
        ParseResult parsed = parseFile(file);

        Map<RuleKey, FeeRuleEntity> rules = activeRules(merchantId);
        List<ImportRowErrorDTO> errors = new ArrayList<>(parsed.errors());
        Map<ParsedTransactionRow, BigDecimal> expectedNet = new HashMap<>();
        for (ParsedTransactionRow row : parsed.rows()) {
            FeeRuleEntity rule = rules.get(new RuleKey(row.paymentMethod(), row.installments()));
            if (rule == null) {
                errors.add(new ImportRowErrorDTO(row.rowNumber(),
                        "Regra de taxa ativa não encontrada para " + row.paymentMethod()
                                + " em " + row.installments() + "x"));
                continue;
            }
            try {
                expectedNet.put(row, ExpectedNetAmountCalculator.calculate(row.amount(), rule));
            } catch (InvalidTransactionAmountException ex) {
                errors.add(new ImportRowErrorDTO(row.rowNumber(), ex.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            // Stable sort: a row keeps its format errors before its fee-rule error.
            errors.sort(Comparator.comparingInt(ImportRowErrorDTO::row));
            throw new TransactionImportValidationException("Erro na importação do CSV", errors);
        }

        ensureNoReferenceAlreadyStored(merchantId, parsed.rows());
        periodGuard.assertDatesOpen(
                merchantId,
                parsed.rows().stream().map(ParsedTransactionRow::transactionDate).toList());

        TransactionImportEntity importBatch = new TransactionImportEntity();
        importBatch.setMerchant(merchant);
        importBatch.setFileName(file.getOriginalFilename());
        importBatch.setTotalRows(parsed.rows().size());
        TransactionImportEntity savedImport = transactionImportRepository.save(importBatch);

        transactionRepository.saveAll(parsed.rows().stream()
                .map(row -> toEntity(merchant, savedImport, row, expectedNet.get(row)))
                .toList());
        auditLogger.record("TRANSACTIONS_IMPORTED", "transactionImport", savedImport.getId(),
                "merchant=" + merchantId + " rows=" + parsed.rows().size());

        return transactionImportMapper.toDTO(savedImport);
    }

    @Transactional(readOnly = true)
    public Page<TransactionImportResponseDTO> findAllImports(UUID merchantId, Pageable pageable) {
        ensureMerchantExists(merchantId);
        return transactionImportRepository.findAllByMerchant_Id(merchantId, pageable)
                .map(transactionImportMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public TransactionImportResponseDTO findImportById(UUID merchantId, UUID importId) {
        ensureMerchantExists(merchantId);
        return transactionImportRepository.findByIdAndMerchant_Id(importId, merchantId)
                .map(transactionImportMapper::toDTO)
                .orElseThrow(() -> new TransactionImportNotFoundException(
                        "Importação não encontrada com id: " + importId));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidTransactionImportException("Arquivo CSV é obrigatório");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
            throw new InvalidTransactionImportException("Arquivo deve ser um CSV (.csv)");
        }
    }

    private ParseResult parseFile(MultipartFile file) {
        try {
            return transactionCsvParser.parse(file.getInputStream());
        } catch (IOException ex) {
            throw new InvalidTransactionImportException("Erro ao ler arquivo CSV");
        }
    }

    private Map<RuleKey, FeeRuleEntity> activeRules(UUID merchantId) {
        Map<RuleKey, FeeRuleEntity> rules = new HashMap<>();
        for (FeeRuleEntity rule : feeRuleRepository.findAllByMerchant_IdAndActiveTrue(merchantId)) {
            rules.put(new RuleKey(rule.getPaymentMethod(), rule.getInstallments()), rule);
        }
        return rules;
    }

    private void ensureNoReferenceAlreadyStored(UUID merchantId, List<ParsedTransactionRow> rows) {
        List<String> references = rows.stream().map(ParsedTransactionRow::externalReference).toList();
        Set<String> stored = transactionRepository.findExistingReferences(merchantId, references);
        // In file order, so the answer reads like the file.
        List<String> conflicting = references.stream().filter(stored::contains).toList();
        if (!conflicting.isEmpty()) {
            throw new DuplicateTransactionReferenceException(
                    "Referências externas já cadastradas para este comerciante: " + String.join(", ", conflicting),
                    conflicting);
        }
    }

    private InternalTransactionEntity toEntity(
            MerchantEntity merchant,
            TransactionImportEntity importBatch,
            ParsedTransactionRow row,
            BigDecimal expectedNetAmount) {
        InternalTransactionEntity entity = new InternalTransactionEntity();
        entity.setMerchant(merchant);
        entity.setImportBatch(importBatch);
        entity.setExternalReference(row.externalReference());
        entity.setAmount(row.amount());
        entity.setExpectedNetAmount(expectedNetAmount);
        entity.setPaymentMethod(row.paymentMethod());
        entity.setInstallments(row.installments());
        entity.setStatus(row.status());
        entity.setTransactionDate(row.transactionDate());
        return entity;
    }

    private void ensureMerchantExists(UUID merchantId) {
        merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));
    }

    private record RuleKey(PaymentMethod paymentMethod, int installments) {
    }
}
