package br.com.hanrry.reconpay.externalsettlement.service;

import br.com.hanrry.reconpay.exception.InvalidSettlementImportException;
import br.com.hanrry.reconpay.exception.SettlementImportValidationException;
import br.com.hanrry.reconpay.externalsettlement.dto.ImportRowErrorDTO;
import br.com.hanrry.reconpay.externalsettlement.enums.SettlementLayout;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
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

    @Test
    void shouldParseReconpayLayoutIntoSettlementRow() {
        String csv = """
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                TXN-001,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """;

        List<SettlementCsvParser.ParsedSettlementRow> rows = parser.parse(
                toStream(csv),
                SettlementLayout.RECONPAY);

        assertThat(rows).hasSize(1);
        SettlementCsvParser.ParsedSettlementRow row = rows.getFirst();
        assertThat(row.externalReference()).isEqualTo("TXN-001");
        assertThat(row.amount()).isEqualByComparingTo("150.00");
        assertThat(row.netAmount()).isEqualByComparingTo("145.00");
        assertThat(row.paymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(row.installments()).isEqualTo(3);
        assertThat(row.status()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(row.settlementDate()).isEqualTo(LocalDate.parse("2026-07-30"));
    }

    @Test
    void shouldMapAcquirerColumnsInDesignOrder() {
        String csv = """
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                NSU-001,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """;

        List<SettlementCsvParser.ParsedSettlementRow> rows = parser.parse(
                toStream(csv),
                SettlementLayout.ACQUIRER);

        assertThat(rows).hasSize(1);
        SettlementCsvParser.ParsedSettlementRow row = rows.getFirst();
        assertThat(row.externalReference()).isEqualTo("NSU-001");
        assertThat(row.amount()).isEqualByComparingTo("150.00");
        assertThat(row.netAmount()).isEqualByComparingTo("145.00");
        assertThat(row.paymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(row.installments()).isEqualTo(3);
        assertThat(row.status()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(row.settlementDate()).isEqualTo(LocalDate.parse("2026-07-30"));
    }

    @Test
    void shouldRejectReconpayHeaderWhenLayoutIsAcquirer() {
        String csv = """
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                TXN-001,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv), SettlementLayout.ACQUIRER))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage(
                        "Cabeçalho CSV inválido. Esperado: nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao");
    }

    @Test
    void shouldRejectAcquirerHeaderWhenLayoutIsReconpay() {
        String csv = """
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                NSU-001,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv), SettlementLayout.RECONPAY))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage(
                        "Cabeçalho CSV inválido. Esperado: externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate");
    }

    @Test
    void shouldRejectAcquirerRowWhenValueRulesFail() {
        String netAboveGross = """
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                NSU-NET,100.00,150.00,PIX,1,APPROVED,2026-07-30
                """;
        String nonPositiveAmount = """
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                NSU-ZERO,0.00,0.00,PIX,1,APPROVED,2026-07-30
                """;
        String tooManyDecimals = """
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                NSU-SCALE,150.001,145.00,PIX,1,APPROVED,2026-07-30
                """;
        String tooManyIntegers = """
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                NSU-BIG,100000000000000000.00,1.00,PIX,1,APPROVED,2026-07-30
                """;

        assertThatThrownBy(() -> parser.parse(toStream(netAboveGross), SettlementLayout.ACQUIRER))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .extracting(ImportRowErrorDTO::message)
                        .contains("netAmount não pode ser maior que amount"));

        assertThatThrownBy(() -> parser.parse(toStream(nonPositiveAmount), SettlementLayout.ACQUIRER))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .extracting(ImportRowErrorDTO::message)
                        .contains("amount deve ser maior que zero", "netAmount deve ser maior que zero"));

        assertThatThrownBy(() -> parser.parse(toStream(tooManyDecimals), SettlementLayout.ACQUIRER))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .extracting(ImportRowErrorDTO::message)
                        .contains("amount deve ter no máximo 17 dígitos inteiros e 2 casas decimais"));

        assertThatThrownBy(() -> parser.parse(toStream(tooManyIntegers), SettlementLayout.ACQUIRER))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .extracting(ImportRowErrorDTO::message)
                        .contains("amount deve ter no máximo 17 dígitos inteiros e 2 casas decimais"));
    }

    @Test
    void shouldRejectInvalidAcquirerEnums() {
        String invalidPaymentMethod = """
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                NSU-METHOD,150.00,145.00,CASH,1,APPROVED,2026-07-30
                """;
        String invalidStatus = """
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                NSU-STATUS,150.00,145.00,PIX,1,PAID,2026-07-30
                """;

        assertThatThrownBy(() -> parser.parse(toStream(invalidPaymentMethod), SettlementLayout.ACQUIRER))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .extracting(ImportRowErrorDTO::message)
                        .contains("Método de pagamento inválido"));

        assertThatThrownBy(() -> parser.parse(toStream(invalidStatus), SettlementLayout.ACQUIRER))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .extracting(ImportRowErrorDTO::message)
                        .contains("Status inválido"));
    }

    @Test
    void shouldRejectInvalidAcquirerSettlementDate() {
        String futureDate = """
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                NSU-FUTURE,150.00,145.00,PIX,1,APPROVED,2026-08-05
                """;
        String invalidFormat = """
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                NSU-DATE,150.00,145.00,PIX,1,APPROVED,30/07/2026
                """;

        assertThatThrownBy(() -> parser.parse(toStream(futureDate), SettlementLayout.ACQUIRER))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .extracting(ImportRowErrorDTO::message)
                        .contains("Data de liquidação não pode ser futura"));

        assertThatThrownBy(() -> parser.parse(toStream(invalidFormat), SettlementLayout.ACQUIRER))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .extracting(ImportRowErrorDTO::message)
                        .contains("Data de liquidação inválida. Formato esperado: yyyy-MM-dd"));
    }

    @Test
    void shouldRejectDuplicateAcquirerReferenceInFile() {
        String csv = """
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                NSU-001,150.00,145.00,PIX,1,APPROVED,2026-07-30
                NSU-001,80.00,75.00,PIX,1,APPROVED,2026-07-30
                """;

        assertThatThrownBy(() -> parser.parse(toStream(csv), SettlementLayout.ACQUIRER))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> {
                    SettlementImportValidationException validationException = (SettlementImportValidationException) ex;
                    assertThat(validationException.getRowErrors())
                            .extracting(ImportRowErrorDTO::row, ImportRowErrorDTO::message)
                            .containsExactly(tuple(
                                    3,
                                    "Referência externa duplicada no arquivo: NSU-001"));
                });
    }

    @Test
    void shouldRejectEmptySettlementFileWithoutReturningRows() {
        assertThatThrownBy(() -> parser.parse(toStream(""), SettlementLayout.RECONPAY))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage("Arquivo CSV vazio");

        assertThatThrownBy(() -> parser.parse(toStream(""), SettlementLayout.ACQUIRER))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage("Arquivo CSV vazio");
    }

    private ByteArrayInputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
