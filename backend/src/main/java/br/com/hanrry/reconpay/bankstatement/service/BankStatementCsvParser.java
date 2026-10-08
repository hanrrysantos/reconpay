package br.com.hanrry.reconpay.bankstatement.service;

import br.com.hanrry.reconpay.exception.InvalidSettlementImportException;
import br.com.hanrry.reconpay.exception.SettlementImportValidationException;
import br.com.hanrry.reconpay.externalsettlement.dto.ImportRowErrorDTO;
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

@Component
@RequiredArgsConstructor
public class BankStatementCsvParser {

    private static final String[] EXPECTED_HEADER = {
            "lineReference",
            "externalReference",
            "amount",
            "movementDate"
    };

    private static final DateTimeFormatter MOVEMENT_DATE_FORMAT = DateTimeFormatter
            .ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    private final Clock clock;

    public List<ParsedBankStatementRow> parse(InputStream inputStream) {
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] headerColumns = csvReader.readNext();
            if (headerColumns == null || isBlankRow(headerColumns)) {
                throw new InvalidSettlementImportException("Arquivo CSV vazio");
            }

            validateHeader(headerColumns);

            List<ParsedBankStatementRow> rows = new ArrayList<>();
            List<ImportRowErrorDTO> errors = new ArrayList<>();
            Set<String> referencesInFile = new HashSet<>();
            String[] columns;
            int rowNumber = 1;

            while ((columns = csvReader.readNext()) != null) {
                rowNumber++;

                if (isBlankRow(columns)) {
                    continue;
                }

                if (columns.length != EXPECTED_HEADER.length) {
                    errors.add(new ImportRowErrorDTO(
                            rowNumber,
                            "Número de colunas inválido. Esperado: " + EXPECTED_HEADER.length));
                    continue;
                }

                ParsedBankStatementRow parsedRow = validateRow(rowNumber, columns, referencesInFile, errors);
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
        } catch (IOException | CsvValidationException ex) {
            throw new InvalidSettlementImportException("Erro ao ler arquivo CSV");
        }
    }

    private void validateHeader(String[] headerColumns) {
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

    private ParsedBankStatementRow validateRow(
            int rowNumber,
            String[] columns,
            Set<String> referencesInFile,
            List<ImportRowErrorDTO> errors) {
        String lineReference = columns[0].trim();
        String externalReferenceRaw = columns[1].trim();
        String amountRaw = columns[2].trim();
        String movementDateRaw = columns[3].trim();

        boolean hasError = false;

        if (lineReference.isBlank()) {
            errors.add(new ImportRowErrorDTO(rowNumber, "Referência da linha é obrigatória"));
            hasError = true;
        } else {
            if (lineReference.length() > 100) {
                errors.add(new ImportRowErrorDTO(
                        rowNumber,
                        "Referência da linha deve ter no máximo 100 caracteres"));
                hasError = true;
            }

            if (!referencesInFile.add(lineReference)) {
                errors.add(new ImportRowErrorDTO(
                        rowNumber,
                        "Referência da linha duplicada no arquivo: " + lineReference));
                hasError = true;
            }
        }

        String externalReference = externalReferenceRaw.isBlank() ? null : externalReferenceRaw;
        if (externalReference != null && externalReference.length() > 100) {
            errors.add(new ImportRowErrorDTO(
                    rowNumber,
                    "Referência externa deve ter no máximo 100 caracteres"));
            hasError = true;
        }

        BigDecimal amount = parsePositiveAmount(rowNumber, amountRaw, errors);
        if (amount == null) {
            hasError = true;
        }

        LocalDate movementDate = parseMovementDate(rowNumber, movementDateRaw, errors);
        if (movementDate == null) {
            hasError = true;
        }

        if (hasError) {
            return null;
        }

        return new ParsedBankStatementRow(lineReference, externalReference, amount, movementDate);
    }

    private BigDecimal parsePositiveAmount(
            int rowNumber,
            String rawValue,
            List<ImportRowErrorDTO> errors) {
        try {
            BigDecimal value = new BigDecimal(rawValue);
            if (value.scale() > 2 || (long) value.precision() - value.scale() > 17) {
                errors.add(new ImportRowErrorDTO(
                        rowNumber,
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

    private LocalDate parseMovementDate(
            int rowNumber,
            String rawValue,
            List<ImportRowErrorDTO> errors) {
        try {
            LocalDate movementDate = LocalDate.parse(rawValue, MOVEMENT_DATE_FORMAT);
            if (movementDate.isAfter(LocalDate.now(clock))) {
                errors.add(new ImportRowErrorDTO(
                        rowNumber,
                        "Data de movimentação não pode ser futura"));
                return null;
            }
            return movementDate;
        } catch (DateTimeParseException ex) {
            errors.add(new ImportRowErrorDTO(
                    rowNumber,
                    "Data de movimentação inválida. Formato esperado: yyyy-MM-dd"));
            return null;
        }
    }

    private boolean isBlankRow(String[] columns) {
        return Arrays.stream(columns).allMatch(column -> column == null || column.isBlank());
    }

    public record ParsedBankStatementRow(
            String lineReference,
            String externalReference,
            BigDecimal amount,
            LocalDate movementDate
    ) {
    }
}
