package br.com.hanrry.reconpay.externalSettlement.service;

import br.com.hanrry.reconpay.exception.InvalidSettlementImportException;
import br.com.hanrry.reconpay.exception.SettlementImportValidationException;
import br.com.hanrry.reconpay.externalSettlement.dto.ImportRowErrorDTO;
import br.com.hanrry.reconpay.shared.PaymentMethodRules;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class SettlementCsvParser {

    private static final String[] EXPECTED_HEADER = {
            "externalReference",
            "amount",
            "netAmount",
            "paymentMethod",
            "installments",
            "status",
            "settlementDate"
    };

    public List<ParsedSettlementRow> parse(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new InvalidSettlementImportException("Arquivo CSV vazio");
            }

            validateHeader(headerLine.trim());

            List<ParsedSettlementRow> rows = new ArrayList<>();
            List<ImportRowErrorDTO> errors = new ArrayList<>();
            Set<String> referencesInFile = new HashSet<>();
            String line;
            int rowNumber = 1;

            while ((line = reader.readLine()) != null) {
                rowNumber++;

                if (line.isBlank()) {
                    continue;
                }

                String[] columns = line.split(",", -1);
                if (columns.length != EXPECTED_HEADER.length) {
                    errors.add(new ImportRowErrorDTO(
                            rowNumber,
                            "Número de colunas inválido. Esperado: " + EXPECTED_HEADER.length));
                    continue;
                }

                ParsedSettlementRow parsedRow = validateRow(
                        rowNumber,
                        columns,
                        referencesInFile,
                        errors);

                if (parsedRow != null) {
                    rows.add(parsedRow);
                }
            }

            if (rows.isEmpty() && errors.isEmpty()) {
                throw new InvalidSettlementImportException("CSV não contém registros");
            }

            if (!errors.isEmpty()) {
                throw new SettlementImportValidationException("Erro na importação do CSV", errors);
            }

            return rows;
        } catch (IOException ex) {
            throw new InvalidSettlementImportException("Erro ao ler arquivo CSV");
        }
    }

    private void validateHeader(String headerLine) {
        String[] headerColumns = headerLine.split(",", -1);
        if (headerColumns.length != EXPECTED_HEADER.length) {
            throw new InvalidSettlementImportException(
                    "Cabeçalho CSV inválido. Esperado: " + String.join(",", EXPECTED_HEADER));
        }

        for (int i = 0; i < EXPECTED_HEADER.length; i++) {
            if (!EXPECTED_HEADER[i].equals(headerColumns[i].trim())) {
                throw new InvalidSettlementImportException(
                        "Cabeçalho CSV inválido. Esperado: " + String.join(",", EXPECTED_HEADER));
            }
        }
    }

    private ParsedSettlementRow validateRow(
            int rowNumber,
            String[] columns,
            Set<String> referencesInFile,
            List<ImportRowErrorDTO> errors) {
        String externalReference = columns[0].trim();
        String amountRaw = columns[1].trim();
        String netAmountRaw = columns[2].trim();
        String paymentMethodRaw = columns[3].trim();
        String installmentsRaw = columns[4].trim();
        String statusRaw = columns[5].trim();
        String settlementDateRaw = columns[6].trim();

        if (externalReference.isBlank()) {
            errors.add(new ImportRowErrorDTO(rowNumber, "Referência externa é obrigatória"));
            return null;
        }

        if (externalReference.length() > 100) {
            errors.add(new ImportRowErrorDTO(rowNumber, "Referência externa deve ter no máximo 100 caracteres"));
            return null;
        }

        if (!referencesInFile.add(externalReference)) {
            errors.add(new ImportRowErrorDTO(
                    rowNumber,
                    "Referência externa duplicada no arquivo: " + externalReference));
            return null;
        }

        BigDecimal amount = parsePositiveAmount(rowNumber, amountRaw, "amount", errors);
        if (amount == null) {
            return null;
        }

        BigDecimal netAmount = parsePositiveAmount(rowNumber, netAmountRaw, "netAmount", errors);
        if (netAmount == null) {
            return null;
        }

        PaymentMethod paymentMethod = parsePaymentMethod(rowNumber, paymentMethodRaw, errors);
        if (paymentMethod == null) {
            return null;
        }

        Integer installments = parseInstallments(rowNumber, installmentsRaw, errors);
        if (installments == null) {
            return null;
        }

        if (!PaymentMethodRules.allowsInstallments(paymentMethod, installments)) {
            errors.add(new ImportRowErrorDTO(
                    rowNumber,
                    "Método de pagamento " + paymentMethod + " não permite parcelamento"));
            return null;
        }

        TransactionStatus status = parseStatus(rowNumber, statusRaw, errors);
        if (status == null) {
            return null;
        }

        LocalDate settlementDate = parseSettlementDate(rowNumber, settlementDateRaw, errors);
        if (settlementDate == null) {
            return null;
        }

        return new ParsedSettlementRow(
                externalReference,
                amount,
                netAmount,
                paymentMethod,
                installments,
                status,
                settlementDate);
    }

    private BigDecimal parsePositiveAmount(
            int rowNumber,
            String rawValue,
            String fieldName,
            List<ImportRowErrorDTO> errors) {
        try {
            BigDecimal value = new BigDecimal(rawValue);
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(new ImportRowErrorDTO(
                        rowNumber,
                        fieldName + " deve ser maior que zero"));
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            errors.add(new ImportRowErrorDTO(rowNumber, fieldName + " inválido"));
            return null;
        }
    }

    private PaymentMethod parsePaymentMethod(
            int rowNumber,
            String rawValue,
            List<ImportRowErrorDTO> errors) {
        try {
            return PaymentMethod.valueOf(rawValue);
        } catch (IllegalArgumentException ex) {
            errors.add(new ImportRowErrorDTO(rowNumber, "Método de pagamento inválido"));
            return null;
        }
    }

    private Integer parseInstallments(
            int rowNumber,
            String rawValue,
            List<ImportRowErrorDTO> errors) {
        try {
            int installments = Integer.parseInt(rawValue);
            if (installments < 1) {
                errors.add(new ImportRowErrorDTO(
                        rowNumber,
                        "Número de parcelas deve ser no mínimo 1"));
                return null;
            }
            return installments;
        } catch (NumberFormatException ex) {
            errors.add(new ImportRowErrorDTO(rowNumber, "Número de parcelas inválido"));
            return null;
        }
    }

    private TransactionStatus parseStatus(
            int rowNumber,
            String rawValue,
            List<ImportRowErrorDTO> errors) {
        try {
            return TransactionStatus.valueOf(rawValue);
        } catch (IllegalArgumentException ex) {
            errors.add(new ImportRowErrorDTO(rowNumber, "Status inválido"));
            return null;
        }
    }

    private LocalDate parseSettlementDate(
            int rowNumber,
            String rawValue,
            List<ImportRowErrorDTO> errors) {
        try {
            LocalDate settlementDate = LocalDate.parse(rawValue);
            if (settlementDate.isAfter(LocalDate.now())) {
                errors.add(new ImportRowErrorDTO(
                        rowNumber,
                        "Data de liquidação não pode ser futura"));
                return null;
            }
            return settlementDate;
        } catch (DateTimeParseException ex) {
            errors.add(new ImportRowErrorDTO(rowNumber, "Data de liquidação inválida"));
            return null;
        }
    }

    public record ParsedSettlementRow(
            String externalReference,
            BigDecimal amount,
            BigDecimal netAmount,
            PaymentMethod paymentMethod,
            Integer installments,
            TransactionStatus status,
            LocalDate settlementDate
    ) {
    }
}
