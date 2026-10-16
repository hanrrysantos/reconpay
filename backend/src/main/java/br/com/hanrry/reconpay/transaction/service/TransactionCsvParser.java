package br.com.hanrry.reconpay.transaction.service;

import br.com.hanrry.reconpay.exception.InvalidTransactionImportException;
import br.com.hanrry.reconpay.externalsettlement.dto.ImportRowErrorDTO;
import br.com.hanrry.reconpay.shared.PaymentMethodRules;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Reads the ReconPay transaction layout. Problems with the file itself throw; problems with a row are
 * collected and returned with the valid rows, so the import service can add the fee-rule errors and
 * answer with every error of every row at once.
 */
@Component
@RequiredArgsConstructor
public class TransactionCsvParser {

    static final String[] HEADER = {
            "externalReference", "amount", "paymentMethod", "installments", "status", "transactionDate"
    };

    private static final DateTimeFormatter TRANSACTION_DATE_FORMAT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private final Clock clock;

    public ParseResult parse(InputStream inputStream) {
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] headerColumns = csvReader.readNext();
            if (headerColumns == null || isBlankRow(headerColumns)) {
                throw new InvalidTransactionImportException("Arquivo CSV vazio");
            }
            validateHeader(headerColumns);

            List<ParsedTransactionRow> rows = new ArrayList<>();
            List<ImportRowErrorDTO> errors = new ArrayList<>();
            Set<String> referencesInFile = new HashSet<>();
            String[] columns;
            int rowNumber = 1;

            while ((columns = csvReader.readNext()) != null) {
                rowNumber++;

                if (isBlankRow(columns)) {
                    continue;
                }

                if (columns.length != HEADER.length) {
                    errors.add(new ImportRowErrorDTO(
                            rowNumber,
                            "Número de colunas inválido. Esperado: " + HEADER.length));
                    continue;
                }

                ParsedTransactionRow row = validateRow(rowNumber, columns, referencesInFile, errors);
                if (row != null) {
                    rows.add(row);
                }
            }

            if (rows.isEmpty() && errors.isEmpty()) {
                throw new InvalidTransactionImportException("CSV não contém registros");
            }

            return new ParseResult(rows, errors);
        } catch (IOException | CsvValidationException ex) {
            throw new InvalidTransactionImportException("Erro ao ler arquivo CSV");
        }
    }

    private void validateHeader(String[] headerColumns) {
        boolean matches = headerColumns.length == HEADER.length;
        for (int i = 0; matches && i < HEADER.length; i++) {
            matches = HEADER[i].equals(headerColumns[i].trim());
        }
        if (!matches) {
            throw new InvalidTransactionImportException(
                    "Cabeçalho CSV inválido. Esperado: " + String.join(",", HEADER));
        }
    }

    private ParsedTransactionRow validateRow(
            int rowNumber,
            String[] columns,
            Set<String> referencesInFile,
            List<ImportRowErrorDTO> errors) {
        String externalReference = columns[0].trim();
        int errorsBefore = errors.size();

        if (externalReference.isBlank()) {
            errors.add(new ImportRowErrorDTO(rowNumber, "Referência externa é obrigatória"));
        } else {
            if (externalReference.length() > 100) {
                errors.add(new ImportRowErrorDTO(
                        rowNumber,
                        "Referência externa deve ter no máximo 100 caracteres"));
            }
            if (!referencesInFile.add(externalReference)) {
                errors.add(new ImportRowErrorDTO(
                        rowNumber,
                        "Referência externa duplicada no arquivo: " + externalReference));
            }
        }

        BigDecimal amount = parsePositiveAmount(rowNumber, columns[1].trim(), errors);
        PaymentMethod paymentMethod = parsePaymentMethod(rowNumber, columns[2].trim(), errors);
        Integer installments = parseInstallments(rowNumber, columns[3].trim(), errors);

        if (paymentMethod != null
                && installments != null
                && !PaymentMethodRules.allowsInstallments(paymentMethod, installments)) {
            errors.add(new ImportRowErrorDTO(
                    rowNumber,
                    "Método de pagamento " + paymentMethod + " não permite parcelamento"));
        }

        TransactionStatus status = parseStatus(rowNumber, columns[4].trim(), errors);
        LocalDate transactionDate = parseTransactionDate(rowNumber, columns[5].trim(), errors);

        if (errors.size() > errorsBefore) {
            return null;
        }

        return new ParsedTransactionRow(
                rowNumber, externalReference, amount, paymentMethod, installments, status, transactionDate);
    }

    private BigDecimal parsePositiveAmount(int rowNumber, String rawValue, List<ImportRowErrorDTO> errors) {
        try {
            BigDecimal value = new BigDecimal(rawValue);
            if (value.scale() > 2 || (long) value.precision() - value.scale() > 17) {
                errors.add(new ImportRowErrorDTO(rowNumber,
                        "amount deve ter no máximo 17 dígitos inteiros e 2 casas decimais"));
                return null;
            }
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(new ImportRowErrorDTO(rowNumber, "amount deve ser maior que zero"));
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            errors.add(new ImportRowErrorDTO(rowNumber, "amount inválido"));
            return null;
        }
    }

    private PaymentMethod parsePaymentMethod(int rowNumber, String rawValue, List<ImportRowErrorDTO> errors) {
        try {
            return PaymentMethod.valueOf(rawValue);
        } catch (IllegalArgumentException ex) {
            errors.add(new ImportRowErrorDTO(rowNumber, "Método de pagamento inválido"));
            return null;
        }
    }

    private Integer parseInstallments(int rowNumber, String rawValue, List<ImportRowErrorDTO> errors) {
        try {
            int installments = Integer.parseInt(rawValue);
            if (installments < 1) {
                errors.add(new ImportRowErrorDTO(rowNumber, "Número de parcelas deve ser no mínimo 1"));
                return null;
            }
            return installments;
        } catch (NumberFormatException ex) {
            errors.add(new ImportRowErrorDTO(rowNumber, "Número de parcelas inválido"));
            return null;
        }
    }

    private TransactionStatus parseStatus(int rowNumber, String rawValue, List<ImportRowErrorDTO> errors) {
        try {
            return TransactionStatus.valueOf(rawValue);
        } catch (IllegalArgumentException ex) {
            errors.add(new ImportRowErrorDTO(rowNumber, "Status inválido"));
            return null;
        }
    }

    private LocalDate parseTransactionDate(int rowNumber, String rawValue, List<ImportRowErrorDTO> errors) {
        try {
            LocalDate transactionDate = LocalDate.parse(rawValue, TRANSACTION_DATE_FORMAT);
            if (transactionDate.isAfter(LocalDate.now(clock))) {
                errors.add(new ImportRowErrorDTO(rowNumber, "Data da transação não pode ser futura"));
                return null;
            }
            return transactionDate;
        } catch (DateTimeParseException ex) {
            errors.add(new ImportRowErrorDTO(
                    rowNumber,
                    "Data da transação inválida. Formato esperado: yyyy-MM-dd"));
            return null;
        }
    }

    private boolean isBlankRow(String[] columns) {
        return Arrays.stream(columns).allMatch(column -> column == null || column.isBlank());
    }

    public record ParseResult(List<ParsedTransactionRow> rows, List<ImportRowErrorDTO> errors) {
    }

    public record ParsedTransactionRow(
            int rowNumber,
            String externalReference,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            int installments,
            TransactionStatus status,
            LocalDate transactionDate
    ) {
    }
}
