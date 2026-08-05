package br.com.hanrry.reconpay.externalsettlement.service;

import br.com.hanrry.reconpay.exception.InvalidSettlementImportException;
import br.com.hanrry.reconpay.exception.SettlementImportValidationException;
import br.com.hanrry.reconpay.externalsettlement.dto.ImportRowErrorDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettlementCsvParserTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-08-01T12:00:00Z"),
            ZoneOffset.UTC);

    private SettlementCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new SettlementCsvParser(FIXED_CLOCK);
    }

    @Test
    void shouldParseValidCsvRow() {
        String csv = """
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                TXN-001,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """;

        List<SettlementCsvParser.ParsedSettlementRow> rows = parser.parse(toStream(csv));

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().externalReference()).isEqualTo("TXN-001");
        assertThat(rows.getFirst().netAmount()).isEqualByComparingTo("145.00");
    }

    @Test
    void shouldParseQuotedExternalReferenceWithComma() {
        String csv = """
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                "TXN-001,BR",150.00,145.00,PIX,1,APPROVED,2026-07-30
                """;

        List<SettlementCsvParser.ParsedSettlementRow> rows = parser.parse(toStream(csv));

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().externalReference()).isEqualTo("TXN-001,BR");
    }

    @Test
    void shouldRejectFutureSettlementDateUsingInjectedClock() {
        String csv = """
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                TXN-FUTURE,150.00,145.00,PIX,1,APPROVED,2026-08-05
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> {
                    SettlementImportValidationException validationException = (SettlementImportValidationException) ex;
                    assertThat(validationException.getRowErrors())
                            .extracting(ImportRowErrorDTO::message)
                            .contains("Data de liquidação não pode ser futura");
                });
    }

    @Test
    void shouldRejectInvalidSettlementDateFormat() {
        String csv = """
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                TXN-DATE,150.00,145.00,PIX,1,APPROVED,30/07/2026
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> {
                    SettlementImportValidationException validationException = (SettlementImportValidationException) ex;
                    assertThat(validationException.getRowErrors())
                            .extracting(ImportRowErrorDTO::message)
                            .contains("Data de liquidação inválida. Formato esperado: yyyy-MM-dd");
                });
    }

    @Test
    void shouldRejectNetAmountGreaterThanAmount() {
        String csv = """
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                TXN-NET,100.00,150.00,PIX,1,APPROVED,2026-07-30
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> {
                    SettlementImportValidationException validationException = (SettlementImportValidationException) ex;
                    assertThat(validationException.getRowErrors())
                            .extracting(ImportRowErrorDTO::message)
                            .contains("netAmount não pode ser maior que amount");
                });
    }

    @Test
    void shouldAccumulateMultipleErrorsForSameRow() {
        String csv = """
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                ,100.00,150.00,INVALID,0,UNKNOWN,30/07/2026
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> {
                    SettlementImportValidationException validationException = (SettlementImportValidationException) ex;
                    List<ImportRowErrorDTO> rowErrors = validationException.getRowErrors();

                    assertThat(rowErrors).allMatch(error -> error.row() == 2);
                    assertThat(rowErrors).hasSizeGreaterThanOrEqualTo(6);
                    assertThat(rowErrors)
                            .extracting(ImportRowErrorDTO::message)
                            .contains(
                                    "Referência externa é obrigatória",
                                    "netAmount não pode ser maior que amount",
                                    "Método de pagamento inválido",
                                    "Número de parcelas deve ser no mínimo 1",
                                    "Status inválido",
                                    "Data de liquidação inválida. Formato esperado: yyyy-MM-dd");
                });
    }

    @Test
    void shouldRejectInvalidHeader() {
        String csv = """
                ref,valor,liquido,metodo,parcelas,status,data
                TXN-1,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv)))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessageContaining("Cabeçalho CSV inválido");
    }

    private ByteArrayInputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
