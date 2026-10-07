package br.com.hanrry.reconpay.bankstatement.service;

import br.com.hanrry.reconpay.exception.InvalidSettlementImportException;
import br.com.hanrry.reconpay.exception.SettlementImportValidationException;
import br.com.hanrry.reconpay.externalsettlement.dto.ImportRowErrorDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class BankStatementCsvParserTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-01T12:00:00Z"),
            ZoneOffset.UTC);

    private static final String HEADER = "lineReference,externalReference,amount,movementDate";

    private BankStatementCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new BankStatementCsvParser(FIXED_CLOCK);
    }

    @Test
    void shouldStoreBlankExternalReferenceAsNullAndKeepTheOtherFields() {
        String csv = """
                lineReference,externalReference,amount,movementDate
                LN-001,,150.50,2026-07-30
                LN-002,TXN-002,80.00,2026-08-01
                """;

        List<BankStatementCsvParser.ParsedBankStatementRow> rows = parser.parse(toStream(csv));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).lineReference()).isEqualTo("LN-001");
        assertThat(rows.get(0).externalReference()).isNull();
        assertThat(rows.get(0).amount()).isEqualByComparingTo("150.50");
        assertThat(rows.get(0).movementDate()).isEqualTo(LocalDate.parse("2026-07-30"));
        assertThat(rows.get(1).lineReference()).isEqualTo("LN-002");
        assertThat(rows.get(1).externalReference()).isEqualTo("TXN-002");
        assertThat(rows.get(1).amount()).isEqualByComparingTo("80.00");
        assertThat(rows.get(1).movementDate()).isEqualTo(LocalDate.parse("2026-08-01"));
    }

    @Test
    void shouldTreatWhitespaceExternalReferenceAsNull() {
        String csv = """
                lineReference,externalReference,amount,movementDate
                LN-001,   ,10.00,2026-07-30
                """;

        List<BankStatementCsvParser.ParsedBankStatementRow> rows = parser.parse(toStream(csv));

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().externalReference()).isNull();
        assertThat(rows.getFirst().lineReference()).isEqualTo("LN-001");
    }

    @Test
    void shouldAcceptSeventeenIntegerDigitsAndTwoDecimalPlaces() {
        String csv = """
                lineReference,externalReference,amount,movementDate
                LN-001,TXN-001,99999999999999999.99,2026-07-30
                """;

        List<BankStatementCsvParser.ParsedBankStatementRow> rows = parser.parse(toStream(csv));

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().amount()).isEqualByComparingTo("99999999999999999.99");
    }

    @Test
    void shouldRejectBlankLineReference() {
        String csv = """
                lineReference,externalReference,amount,movementDate
                ,TXN-001,150.00,2026-07-30
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .extracting(ImportRowErrorDTO::row, ImportRowErrorDTO::message)
                        .containsExactly(tuple(2, "Referência da linha é obrigatória")));
    }

    @Test
    void shouldRejectLineReferenceLongerThan100() {
        String lineReference = "L".repeat(101);
        String csv = HEADER + "\n" + lineReference + ",TXN-001,150.00,2026-07-30\n";

        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .extracting(ImportRowErrorDTO::row, ImportRowErrorDTO::message)
                        .contains(tuple(2, "Referência da linha deve ter no máximo 100 caracteres")));
    }

    @Test
    void shouldRejectDuplicateLineReferenceInFile() {
        String csv = """
                lineReference,externalReference,amount,movementDate
                LN-001,TXN-001,150.00,2026-07-30
                LN-001,TXN-002,80.00,2026-07-29
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .extracting(ImportRowErrorDTO::row, ImportRowErrorDTO::message)
                        .containsExactly(tuple(3, "Referência da linha duplicada no arquivo: LN-001")));
    }

    @Test
    void shouldRejectExternalReferenceLongerThan100() {
        String externalReference = "E".repeat(101);
        String csv = HEADER + "\nLN-001," + externalReference + ",150.00,2026-07-30\n";

        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .extracting(ImportRowErrorDTO::row, ImportRowErrorDTO::message)
                        .containsExactly(tuple(
                                2,
                                "Referência externa deve ter no máximo 100 caracteres")));
    }

    @Test
    void shouldRejectNonPositiveAmount() {
        assertRowError(
                """
                        lineReference,externalReference,amount,movementDate
                        LN-ZERO,TXN-001,0,2026-07-30
                        """,
                "amount deve ser maior que zero");
        assertRowError(
                """
                        lineReference,externalReference,amount,movementDate
                        LN-NEG,TXN-001,-1.00,2026-07-30
                        """,
                "amount deve ser maior que zero");
    }

    @Test
    void shouldRejectAmountWithMoreThanSeventeenIntegerDigits() {
        assertRowError(
                """
                        lineReference,externalReference,amount,movementDate
                        LN-001,TXN-001,100000000000000000.00,2026-07-30
                        """,
                "amount deve ter no máximo 17 dígitos inteiros e 2 casas decimais");
    }

    @Test
    void shouldRejectAmountWithMoreThanTwoDecimalPlaces() {
        assertRowError(
                """
                        lineReference,externalReference,amount,movementDate
                        LN-001,TXN-001,10.001,2026-07-30
                        """,
                "amount deve ter no máximo 17 dígitos inteiros e 2 casas decimais");
    }

    @Test
    void shouldRejectNonNumericAmount() {
        assertRowError(
                """
                        lineReference,externalReference,amount,movementDate
                        LN-001,TXN-001,abc,2026-07-30
                        """,
                "amount inválido");
    }

    @Test
    void shouldRejectMovementDateThatIsNotStrictIsoDate() {
        assertRowError(
                """
                        lineReference,externalReference,amount,movementDate
                        LN-001,TXN-001,150.00,30/07/2026
                        """,
                "Data de movimentação inválida. Formato esperado: yyyy-MM-dd");
        assertRowError(
                """
                        lineReference,externalReference,amount,movementDate
                        LN-001,TXN-001,150.00,2026-02-29
                        """,
                "Data de movimentação inválida. Formato esperado: yyyy-MM-dd");
    }

    @Test
    void shouldRejectFutureMovementDate() {
        assertRowError(
                """
                        lineReference,externalReference,amount,movementDate
                        LN-001,TXN-001,150.00,2026-08-02
                        """,
                "Data de movimentação não pode ser futura");
    }

    @Test
    void shouldRejectInvalidHeaderWithoutReturningRows() {
        String csv = """
                ref,codigo,valor,data
                LN-001,TXN-001,150.00,2026-07-30
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage("Cabeçalho CSV inválido. Esperado: " + HEADER);
    }

    @Test
    void shouldRejectHeaderWithWrongColumnCountWithoutReturningRows() {
        String csv = """
                lineReference,externalReference,amount
                LN-001,TXN-001,150.00
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage("Cabeçalho CSV inválido. Esperado: " + HEADER);
    }

    @Test
    void shouldRejectEmptyFileWithoutReturningRows() {
        assertThatThrownBy(() -> parser.parse(toStream("")))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage("Arquivo CSV vazio");
    }

    @Test
    void shouldRejectFileWithoutDataRows() {
        assertThatThrownBy(() -> parser.parse(toStream(HEADER + "\n")))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage("CSV não contém registros");
    }

    @Test
    void shouldRejectRowWithWrongColumnCount() {
        String csv = """
                lineReference,externalReference,amount,movementDate
                LN-001,TXN-001,150.00
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .extracting(ImportRowErrorDTO::row, ImportRowErrorDTO::message)
                        .containsExactly(tuple(2, "Número de colunas inválido. Esperado: 4")));
    }

    private void assertRowError(String csv, String message) {
        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .extracting(ImportRowErrorDTO::message)
                        .contains(message));
    }

    private ByteArrayInputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
