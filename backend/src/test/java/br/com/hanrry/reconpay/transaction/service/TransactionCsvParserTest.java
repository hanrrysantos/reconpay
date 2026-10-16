package br.com.hanrry.reconpay.transaction.service;

import br.com.hanrry.reconpay.exception.InvalidTransactionImportException;
import br.com.hanrry.reconpay.externalsettlement.dto.ImportRowErrorDTO;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import br.com.hanrry.reconpay.transaction.service.TransactionCsvParser.ParseResult;
import br.com.hanrry.reconpay.transaction.service.TransactionCsvParser.ParsedTransactionRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class TransactionCsvParserTest {

    private static final String HEADER = "externalReference,amount,paymentMethod,installments,status,transactionDate";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-20T12:00:00Z"), ZoneOffset.UTC);

    private final TransactionCsvParser parser = new TransactionCsvParser(CLOCK);

    private ParseResult parse(String... lines) {
        String csv = String.join("\n", lines);
        return parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    /** One data row with a single field replaced, the rest valid. */
    private static String row(String reference, String amount, String method, String installments, String status, String date) {
        return String.join(",", reference, amount, method, installments, status, date);
    }

    @Test
    void readsEveryFieldOfAValidRow() {
        ParseResult result = parse(HEADER, "PED-1,100.50,CREDIT_CARD,3,APPROVED,2026-07-10");

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).containsExactly(new ParsedTransactionRow(
                2, "PED-1", new BigDecimal("100.50"), PaymentMethod.CREDIT_CARD, 3,
                TransactionStatus.APPROVED, LocalDate.of(2026, 7, 10)));
    }

    @ParameterizedTest
    @CsvSource({"CREDIT_CARD, 1", "CREDIT_CARD, 12", "DEBIT_CARD, 1", "PIX, 1", "BOLETO, 1"})
    void acceptsEveryMethodWithAnAllowedInstallmentCount(PaymentMethod method, int installments) {
        ParseResult result = parse(HEADER, row("PED-1", "10.00", method.name(), String.valueOf(installments), "APPROVED", "2026-07-10"));

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).extracting(ParsedTransactionRow::paymentMethod, ParsedTransactionRow::installments)
                .containsExactly(tuple(method, installments));
    }

    @ParameterizedTest
    @EnumSource(TransactionStatus.class)
    void acceptsEveryStatus(TransactionStatus status) {
        ParseResult result = parse(HEADER, row("PED-1", "10.00", "PIX", "1", status.name(), "2026-07-10"));

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).extracting(ParsedTransactionRow::status).containsExactly(status);
    }

    @Test
    void acceptsToday() {
        assertThat(parse(HEADER, row("PED-1", "10.00", "PIX", "1", "APPROVED", "2026-07-20")).errors()).isEmpty();
    }

    @Test
    void trimsEveryField() {
        ParseResult result = parse(" externalReference , amount ,paymentMethod,installments,status,transactionDate",
                "  PED-1 , 100.00 , PIX , 1 , APPROVED , 2026-07-10 ");

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).extracting(ParsedTransactionRow::externalReference, ParsedTransactionRow::amount)
                .containsExactly(tuple("PED-1", new BigDecimal("100.00")));
    }

    @Test
    void skipsBlankLinesAndKeepsTheRealLineNumbers() {
        ParseResult result = parse(HEADER, "", row("PED-1", "10.00", "PIX", "1", "APPROVED", "2026-07-10"), ",,,,,",
                row("PED-2", "0", "PIX", "1", "APPROVED", "2026-07-10"), "");

        assertThat(result.rows()).extracting(ParsedTransactionRow::rowNumber).containsExactly(3);
        assertThat(result.errors()).containsExactly(new ImportRowErrorDTO(5, "amount deve ser maior que zero"));
    }

    @Test
    void rejectsAWrongHeader() {
        assertThatThrownBy(() -> parse("externalReference,amount,paymentMethod,installments,transactionDate", "x"))
                .isInstanceOf(InvalidTransactionImportException.class)
                .hasMessage("Cabeçalho CSV inválido. Esperado: " + HEADER);
        assertThatThrownBy(() -> parse("externalReference,valor,paymentMethod,installments,status,transactionDate", "x"))
                .isInstanceOf(InvalidTransactionImportException.class)
                .hasMessage("Cabeçalho CSV inválido. Esperado: " + HEADER);
        assertThatThrownBy(() -> parse(HEADER + ",observacao", "x"))
                .isInstanceOf(InvalidTransactionImportException.class)
                .hasMessage("Cabeçalho CSV inválido. Esperado: " + HEADER);
    }

    @Test
    void rejectsAFileWithOnlyTheHeader() {
        assertThatThrownBy(() -> parse(HEADER))
                .isInstanceOf(InvalidTransactionImportException.class)
                .hasMessage("CSV não contém registros");
        assertThatThrownBy(() -> parse(HEADER, "", " "))
                .isInstanceOf(InvalidTransactionImportException.class)
                .hasMessage("CSV não contém registros");
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "colunas a menos|PED-1,10.00,PIX,1,APPROVED|Número de colunas inválido. Esperado: 6",
            "colunas a mais|PED-1,10.00,PIX,1,APPROVED,2026-07-10,x|Número de colunas inválido. Esperado: 6",
            "referência vazia|,10.00,PIX,1,APPROVED,2026-07-10|Referência externa é obrigatória",
            "amount texto|PED-1,dez,PIX,1,APPROVED,2026-07-10|amount inválido",
            "amount 3 casas|PED-1,10.001,PIX,1,APPROVED,2026-07-10|amount deve ter no máximo 17 dígitos inteiros e 2 casas decimais",
            "amount 18 inteiros|PED-1,123456789012345678.00,PIX,1,APPROVED,2026-07-10|amount deve ter no máximo 17 dígitos inteiros e 2 casas decimais",
            "amount zero|PED-1,0.00,PIX,1,APPROVED,2026-07-10|amount deve ser maior que zero",
            "amount negativo|PED-1,-5.00,PIX,1,APPROVED,2026-07-10|amount deve ser maior que zero",
            "método desconhecido|PED-1,10.00,CHEQUE,1,APPROVED,2026-07-10|Método de pagamento inválido",
            "método minúsculo|PED-1,10.00,pix,1,APPROVED,2026-07-10|Método de pagamento inválido",
            "parcelas texto|PED-1,10.00,PIX,um,APPROVED,2026-07-10|Número de parcelas inválido",
            "parcelas decimal|PED-1,10.00,PIX,1.5,APPROVED,2026-07-10|Número de parcelas inválido",
            "parcelas zero|PED-1,10.00,CREDIT_CARD,0,APPROVED,2026-07-10|Número de parcelas deve ser no mínimo 1",
            "pix parcelado|PED-1,10.00,PIX,2,APPROVED,2026-07-10|Método de pagamento PIX não permite parcelamento",
            "boleto parcelado|PED-1,10.00,BOLETO,3,APPROVED,2026-07-10|Método de pagamento BOLETO não permite parcelamento",
            "débito parcelado|PED-1,10.00,DEBIT_CARD,2,APPROVED,2026-07-10|Método de pagamento DEBIT_CARD não permite parcelamento",
            "status desconhecido|PED-1,10.00,PIX,1,PAID,2026-07-10|Status inválido",
            "data formato|PED-1,10.00,PIX,1,APPROVED,10/07/2026|Data da transação inválida. Formato esperado: yyyy-MM-dd",
            "data inexistente|PED-1,10.00,PIX,1,APPROVED,2026-02-30|Data da transação inválida. Formato esperado: yyyy-MM-dd",
            "data futura|PED-1,10.00,PIX,1,APPROVED,2026-07-21|Data da transação não pode ser futura"
    })
    void reportsEachRowErrorWithItsLineAndText(String scenario, String line, String message) {
        ParseResult result = parse(HEADER, row("OK-1", "10.00", "PIX", "1", "APPROVED", "2026-07-10"), line);

        assertThat(result.errors()).containsExactly(new ImportRowErrorDTO(3, message));
        assertThat(result.rows()).extracting(ParsedTransactionRow::externalReference).containsExactly("OK-1");
    }

    @ParameterizedTest
    @CsvSource({"12345678901234567.00", "12345678901234567", "0.01"})
    void acceptsAnAmountAtTheSeventeenIntegerDigitBound(String amount) {
        ParseResult result = parse(HEADER, row("PED-1", amount, "PIX", "1", "APPROVED", "2026-07-10"));

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).extracting(ParsedTransactionRow::amount).containsExactly(new BigDecimal(amount));
    }

    @Test
    void aRepeatedReferenceThatIsAlsoTooLongReportsBothErrors() {
        String reference = "A".repeat(101);
        ParseResult result = parse(HEADER,
                row(reference, "10.00", "PIX", "1", "APPROVED", "2026-07-10"),
                row(reference, "10.00", "PIX", "1", "APPROVED", "2026-07-10"));

        assertThat(result.errors()).containsExactly(
                new ImportRowErrorDTO(2, "Referência externa deve ter no máximo 100 caracteres"),
                new ImportRowErrorDTO(3, "Referência externa deve ter no máximo 100 caracteres"),
                new ImportRowErrorDTO(3, "Referência externa duplicada no arquivo: " + reference));
    }

    @Test
    void rejectsAReferenceLongerThan100Characters() {
        ParseResult result = parse(HEADER, row("A".repeat(101), "10.00", "PIX", "1", "APPROVED", "2026-07-10"),
                row("B".repeat(100), "10.00", "PIX", "1", "APPROVED", "2026-07-10"));

        assertThat(result.errors()).containsExactly(
                new ImportRowErrorDTO(2, "Referência externa deve ter no máximo 100 caracteres"));
        assertThat(result.rows()).hasSize(1);
    }

    @Test
    void marksEveryRepeatedReferenceFromTheSecondOccurrenceEvenWhenTheFirstHasAnotherError() {
        ParseResult result = parse(HEADER,
                row("PED-1", "0", "PIX", "1", "APPROVED", "2026-07-10"),
                row("PED-1", "10.00", "PIX", "1", "APPROVED", "2026-07-10"),
                row("PED-1", "10.00", "PIX", "1", "APPROVED", "2026-07-10"));

        assertThat(result.errors()).containsExactly(
                new ImportRowErrorDTO(2, "amount deve ser maior que zero"),
                new ImportRowErrorDTO(3, "Referência externa duplicada no arquivo: PED-1"),
                new ImportRowErrorDTO(4, "Referência externa duplicada no arquivo: PED-1"));
        assertThat(result.rows()).isEmpty();
    }

    @Test
    void reportsEveryErrorOfARowAndOfEveryRow() {
        ParseResult result = parse(HEADER,
                row("PED-1", "x", "CHEQUE", "0", "PAID", "ontem"),
                row("PED-2", "10.00", "PIX", "3", "APPROVED", "2026-07-10"));

        assertThat(result.errors()).containsExactly(
                new ImportRowErrorDTO(2, "amount inválido"),
                new ImportRowErrorDTO(2, "Método de pagamento inválido"),
                new ImportRowErrorDTO(2, "Número de parcelas deve ser no mínimo 1"),
                new ImportRowErrorDTO(2, "Status inválido"),
                new ImportRowErrorDTO(2, "Data da transação inválida. Formato esperado: yyyy-MM-dd"),
                new ImportRowErrorDTO(3, "Método de pagamento PIX não permite parcelamento"));
        assertThat(result.rows()).isEmpty();
    }
}
