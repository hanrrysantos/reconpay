package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.bankstatement.entity.BankStatementLineEntity;
import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAmountCalculatorTest {

    private static final LocalDate SETTLEMENT_DATE = LocalDate.parse("2026-07-30");

    private final OpenAmountCalculator calculator = new OpenAmountCalculator();

    @Test
    void shouldAddExpectedNetAmountForOpenMissingSettlement() {
        ReconciliationDiscrepancyEntity discrepancy = open(
                DiscrepancyType.MISSING_SETTLEMENT,
                item("97.00", "10.00", "20.00", "30.00"));

        assertThat(calculator.sum(List.of(discrepancy))).isEqualTo(new BigDecimal("97.00"));
    }

    @Test
    void shouldAddSettlementNetAmountForOpenOrphanSettlement() {
        ReconciliationDiscrepancyEntity discrepancy = open(
                DiscrepancyType.ORPHAN_SETTLEMENT,
                item("10.00", "80.00", "20.00", "30.00"));

        assertThat(calculator.sum(List.of(discrepancy))).isEqualTo(new BigDecimal("80.00"));
    }

    @Test
    void shouldAddAbsoluteAmountDifferenceForOpenIncorrectAmount() {
        ReconciliationDiscrepancyEntity discrepancy = open(
                DiscrepancyType.INCORRECT_AMOUNT,
                item("100.00", "97.20", "10.00", "12.40"));

        assertThat(calculator.sum(List.of(discrepancy))).isEqualTo(new BigDecimal("2.40"));
    }

    @Test
    void shouldAddAbsoluteNetDifferenceForOpenFeeDivergence() {
        ReconciliationDiscrepancyEntity discrepancy = open(
                DiscrepancyType.FEE_DIVERGENCE,
                item("100.00", "97.20", "150.00", "140.00"));

        assertThat(calculator.sum(List.of(discrepancy))).isEqualTo(new BigDecimal("2.80"));
    }

    @Test
    void shouldAddSettlementNetAmountForOpenMissingBankCredit() {
        ReconciliationDiscrepancyEntity discrepancy = open(
                DiscrepancyType.MISSING_BANK_CREDIT,
                item("10.00", "40.00", "20.00", "30.00"));

        assertThat(calculator.sum(List.of(discrepancy))).isEqualTo(new BigDecimal("40.00"));
    }

    @Test
    void shouldAddAbsoluteDifferenceOfSettlementNetAndMatcherBankAmount() {
        ReconciliationDiscrepancyEntity discrepancy = bankMismatchFromMatcher("10.00", "1.225");
        discrepancy.setExpectedValue("0.01");

        assertThat(discrepancy.getActualValue()).isEqualTo("1.23");
        assertThat(calculator.sum(List.of(discrepancy))).isEqualTo(new BigDecimal("8.77"));
    }

    @Test
    void shouldAddMatcherBankAmountForOrphanBankCredit() {
        ReconciliationDiscrepancyEntity discrepancy = orphanBankCreditFromMatcher("1.225");
        discrepancy.setExpectedValue("50.00");

        assertThat(discrepancy.getActualValue()).isEqualTo("1.23");
        assertThat(calculator.sum(List.of(discrepancy))).isEqualTo(new BigDecimal("1.23"));
    }

    @Test
    void shouldAddZeroForOpenStatusMismatch() {
        assertThat(calculator.sum(List.of(open(
                DiscrepancyType.STATUS_MISMATCH,
                item("50.00", "50.00", "50.00", "50.00"))))).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void shouldAddZeroForOpenPaymentMethodMismatch() {
        assertThat(calculator.sum(List.of(open(
                DiscrepancyType.PAYMENT_METHOD_MISMATCH,
                item("50.00", "50.00", "50.00", "50.00"))))).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void shouldAddZeroForOpenInstallmentsMismatch() {
        assertThat(calculator.sum(List.of(open(
                DiscrepancyType.INSTALLMENTS_MISMATCH,
                item("50.00", "50.00", "50.00", "50.00"))))).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void shouldAddZeroForOpenAmbiguousBankMatch() {
        ReconciliationDiscrepancyEntity discrepancy = open(
                DiscrepancyType.AMBIGUOUS_BANK_MATCH,
                item("50.00", "100.00", "50.00", "50.00"));
        discrepancy.setActualValue("40.00");

        assertThat(calculator.sum(List.of(discrepancy))).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void shouldTreatNullAmountsAsZeroBeforeTheDifference() {
        ReconciliationItemEntity missing = item(null, "9.00", "8.00", "7.00");
        ReconciliationItemEntity incorrect = item("9.00", "8.00", null, "3.10");
        ReconciliationItemEntity fee = item(null, "2.50", "9.00", "8.00");
        ReconciliationItemEntity bothNull = item("9.00", "8.00", null, null);

        assertThat(calculator.sum(List.of(open(DiscrepancyType.MISSING_SETTLEMENT, missing))))
                .isEqualTo(new BigDecimal("0.00"));
        assertThat(calculator.sum(List.of(open(DiscrepancyType.INCORRECT_AMOUNT, incorrect))))
                .isEqualTo(new BigDecimal("3.10"));
        assertThat(calculator.sum(List.of(open(DiscrepancyType.FEE_DIVERGENCE, fee))))
                .isEqualTo(new BigDecimal("2.50"));
        assertThat(calculator.sum(List.of(open(DiscrepancyType.INCORRECT_AMOUNT, bothNull))))
                .isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void shouldIgnoreDiscrepanciesThatAreNotOpen() {
        ReconciliationItemEntity item = item("5.00", "5.00", "5.00", "5.00");
        ReconciliationDiscrepancyEntity accepted = open(DiscrepancyType.MISSING_SETTLEMENT, item);
        accepted.setStatus(DiscrepancyStatus.ACCEPTED);
        ReconciliationDiscrepancyEntity adjusted = open(DiscrepancyType.MISSING_SETTLEMENT, item);
        adjusted.setStatus(DiscrepancyStatus.ADJUSTED);
        ReconciliationDiscrepancyEntity writtenOff = open(DiscrepancyType.MISSING_SETTLEMENT, item);
        writtenOff.setStatus(DiscrepancyStatus.WRITTEN_OFF);
        ReconciliationDiscrepancyEntity stillOpen = open(DiscrepancyType.ORPHAN_SETTLEMENT, item("0.00", "1.25", "0.00", "0.00"));

        assertThat(calculator.sum(List.of())).isEqualTo(new BigDecimal("0.00"));
        assertThat(calculator.sum(List.of(accepted, adjusted, writtenOff, stillOpen)))
                .isEqualTo(new BigDecimal("1.25"));
    }

    @Test
    void shouldRoundTheSumHalfUpToScaleTwo() {
        ReconciliationDiscrepancyEntity first = open(
                DiscrepancyType.INCORRECT_AMOUNT,
                item("0.00", "0.00", "0.005", "0.00"));
        ReconciliationDiscrepancyEntity second = open(
                DiscrepancyType.INCORRECT_AMOUNT,
                item("0.00", "0.00", "0.005", "0.00"));
        ReconciliationDiscrepancyEntity single = open(
                DiscrepancyType.INCORRECT_AMOUNT,
                item("0.00", "0.00", "1.005", "0.00"));

        assertThat(calculator.sum(List.of(single))).isEqualTo(new BigDecimal("1.01"));
        assertThat(calculator.sum(List.of(first, second))).isEqualTo(new BigDecimal("0.01"));
    }

    @Test
    void shouldTreatEmptyBankActualValueAsZero() {
        ReconciliationDiscrepancyEntity orphan = open(
                DiscrepancyType.ORPHAN_BANK_CREDIT,
                item("50.00", "50.00", "50.00", "50.00"));
        orphan.setActualValue("");

        ReconciliationDiscrepancyEntity missingActual = open(
                DiscrepancyType.BANK_AMOUNT_MISMATCH,
                item("50.00", "2.50", "50.00", "50.00"));
        missingActual.setActualValue(null);

        assertThat(calculator.sum(List.of(orphan))).isEqualTo(new BigDecimal("0.00"));
        assertThat(calculator.sum(List.of(missingActual))).isEqualTo(new BigDecimal("2.50"));
    }

    private ReconciliationDiscrepancyEntity bankMismatchFromMatcher(String settlementNet, String lineAmount) {
        ReconciliationItemEntity item = new ReconciliationItemEntity();
        item.setExternalReference("TXN-1");
        item.setExternalSettlement(new ExternalSettlementEntity());
        item.setSettlementNetAmount(new BigDecimal(settlementNet));
        item.setSettlementDate(SETTLEMENT_DATE);
        item.setResult(ReconciliationResult.MATCHED);

        BankStatementLineEntity line = new BankStatementLineEntity();
        line.setLineReference("L-1");
        line.setExternalReference("TXN-1");
        line.setAmount(new BigDecimal(lineAmount));
        line.setMovementDate(LocalDate.parse("2026-08-02"));

        matcher().apply(List.of(item), List.of(line), Set.of());
        return item.getDiscrepancies().stream()
                .filter(candidate -> candidate.getType() == DiscrepancyType.BANK_AMOUNT_MISMATCH)
                .findFirst()
                .orElseThrow();
    }

    private ReconciliationDiscrepancyEntity orphanBankCreditFromMatcher(String lineAmount) {
        BankStatementLineEntity line = new BankStatementLineEntity();
        line.setLineReference("L-ORPHAN");
        line.setExternalReference("UNMATCHED");
        line.setAmount(new BigDecimal(lineAmount));
        line.setMovementDate(SETTLEMENT_DATE);

        List<ReconciliationItemEntity> result = matcher().apply(List.of(), List.of(line), Set.of());
        return result.getFirst().getDiscrepancies().stream()
                .filter(candidate -> candidate.getType() == DiscrepancyType.ORPHAN_BANK_CREDIT)
                .findFirst()
                .orElseThrow();
    }

    private BankStatementMatcher matcher() {
        return new BankStatementMatcher(
                new ReconciliationProperties(new BigDecimal("0.00"), 5, 366, false, 1, 1));
    }

    private ReconciliationDiscrepancyEntity open(DiscrepancyType type, ReconciliationItemEntity item) {
        ReconciliationDiscrepancyEntity discrepancy = new ReconciliationDiscrepancyEntity();
        discrepancy.setType(type);
        discrepancy.setStatus(DiscrepancyStatus.OPEN);
        discrepancy.setReconciliationItem(item);
        return discrepancy;
    }

    private ReconciliationItemEntity item(
            String expectedNetAmount,
            String settlementNetAmount,
            String transactionAmount,
            String settlementAmount) {
        ReconciliationItemEntity item = new ReconciliationItemEntity();
        item.setExpectedNetAmount(decimal(expectedNetAmount));
        item.setSettlementNetAmount(decimal(settlementNetAmount));
        item.setTransactionAmount(decimal(transactionAmount));
        item.setSettlementAmount(decimal(settlementAmount));
        return item;
    }

    private BigDecimal decimal(String amount) {
        return amount == null ? null : new BigDecimal(amount);
    }
}
