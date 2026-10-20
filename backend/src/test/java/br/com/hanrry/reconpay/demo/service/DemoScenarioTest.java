package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.bankstatement.service.BankStatementCsvParser;
import br.com.hanrry.reconpay.bankstatement.service.BankStatementCsvParser.ParsedBankStatementRow;
import br.com.hanrry.reconpay.externalsettlement.enums.SettlementLayout;
import br.com.hanrry.reconpay.externalsettlement.service.SettlementCsvParser;
import br.com.hanrry.reconpay.externalsettlement.service.SettlementCsvParser.ParsedSettlementRow;
import br.com.hanrry.reconpay.feerule.dto.FeeRuleRequestDTO;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.service.TransactionCsvParser;
import br.com.hanrry.reconpay.transaction.service.TransactionCsvParser.ParseResult;
import br.com.hanrry.reconpay.transaction.service.TransactionCsvParser.ParsedTransactionRow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DemoScenarioTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    // Os parsers recusam datas futuras pelo relógio; aqui o relógio está no fim do dia da criação.
    private static final Clock CLOCK = Clock.fixed(TODAY.atTime(23, 59, 59).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final TransactionCsvParser transactionParser = new TransactionCsvParser(CLOCK);
    private final SettlementCsvParser settlementParser = new SettlementCsvParser(CLOCK);
    private final BankStatementCsvParser bankStatementParser = new BankStatementCsvParser(CLOCK);

    @Test
    void everyCsvShouldPassThroughTheRealParserWithoutRowErrors() {
        DemoScenario scenario = DemoScenario.build(TODAY);

        ParseResult transactions = transactionParser.parse(stream(scenario.transactionsCsv()));
        List<ParsedSettlementRow> settlements = settlementParser.parse(
                stream(scenario.settlementsCsv()), SettlementLayout.RECONPAY);
        List<ParsedBankStatementRow> bankLines = bankStatementParser.parse(stream(scenario.bankStatementCsv()));

        assertThat(transactions.errors()).isEmpty();
        assertThat(transactions.rows()).isNotEmpty();
        assertThat(settlements).isNotEmpty();
        assertThat(bankLines).isNotEmpty();
    }

    @Test
    void everyDateShouldFallWithinTheSixtyDaysBeforeToday() {
        DemoScenario scenario = DemoScenario.build(TODAY);
        LocalDate first = TODAY.minusDays(60);
        LocalDate last = TODAY.minusDays(1);

        List<LocalDate> dates = Stream.of(
                        transactionRows(scenario).stream().map(ParsedTransactionRow::transactionDate),
                        settlementRows(scenario).stream().map(ParsedSettlementRow::settlementDate),
                        bankRows(scenario).stream().map(ParsedBankStatementRow::movementDate))
                .flatMap(s -> s)
                .toList();

        assertThat(dates).isNotEmpty().allSatisfy(date -> assertThat(date).isBetween(first, last));
        assertThat(dates).doesNotContain(TODAY);
        assertThat(scenario.fromDate()).isEqualTo(first);
        assertThat(scenario.toDate()).isEqualTo(last);
    }

    @Test
    void transactionReferencesShouldBeUnique() {
        List<String> references = transactionRows(DemoScenario.build(TODAY)).stream()
                .map(ParsedTransactionRow::externalReference)
                .toList();

        assertThat(references).doesNotHaveDuplicates();
    }

    @Test
    void feeRulesShouldCoverTheFourPaymentMethodsInOneInstallment() {
        List<FeeRuleRequestDTO> feeRules = DemoScenario.build(TODAY).feeRules();

        assertThat(feeRules).extracting(FeeRuleRequestDTO::paymentMethod)
                .containsExactlyInAnyOrder(
                        PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD, PaymentMethod.PIX, PaymentMethod.BOLETO);
        assertThat(feeRules).extracting(FeeRuleRequestDTO::installments).containsOnly(1);
    }

    @Test
    void sameDayShouldProduceIdenticalCsvs() {
        DemoScenario first = DemoScenario.build(TODAY);
        DemoScenario second = DemoScenario.build(TODAY);

        assertThat(second.transactionsCsv()).isEqualTo(first.transactionsCsv());
        assertThat(second.settlementsCsv()).isEqualTo(first.settlementsCsv());
        assertThat(second.bankStatementCsv()).isEqualTo(first.bankStatementCsv());
        assertThat(second.feeRules()).isEqualTo(first.feeRules());
    }

    @Test
    void anotherDayShouldKeepTheSameCountsAndGrossTotal() {
        DemoScenario today = DemoScenario.build(TODAY);
        DemoScenario anotherDay = DemoScenario.build(TODAY.plusDays(37));

        // Os parsers de outro dia precisam de um relógio daquele dia.
        Clock anotherClock = Clock.offset(CLOCK, Duration.ofDays(37));
        List<ParsedTransactionRow> anotherTransactions = new TransactionCsvParser(anotherClock)
                .parse(stream(anotherDay.transactionsCsv())).rows();
        List<ParsedSettlementRow> anotherSettlements = new SettlementCsvParser(anotherClock)
                .parse(stream(anotherDay.settlementsCsv()), SettlementLayout.RECONPAY);
        List<ParsedBankStatementRow> anotherBankLines = new BankStatementCsvParser(anotherClock)
                .parse(stream(anotherDay.bankStatementCsv()));

        assertThat(anotherTransactions).hasSameSizeAs(transactionRows(today));
        assertThat(anotherSettlements).hasSameSizeAs(settlementRows(today));
        assertThat(anotherBankLines).hasSameSizeAs(bankRows(today));
        assertThat(grossTotal(anotherTransactions)).isEqualByComparingTo(grossTotal(transactionRows(today)));
    }

    private List<ParsedTransactionRow> transactionRows(DemoScenario scenario) {
        return transactionParser.parse(stream(scenario.transactionsCsv())).rows();
    }

    private List<ParsedSettlementRow> settlementRows(DemoScenario scenario) {
        return settlementParser.parse(stream(scenario.settlementsCsv()), SettlementLayout.RECONPAY);
    }

    private List<ParsedBankStatementRow> bankRows(DemoScenario scenario) {
        return bankStatementParser.parse(stream(scenario.bankStatementCsv()));
    }

    private static BigDecimal grossTotal(List<ParsedTransactionRow> rows) {
        return rows.stream().map(ParsedTransactionRow::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static InputStream stream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
