package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.DailySummaryRow;
import br.com.hanrry.reconpay.reconciliation.repository.DiscrepancyTypeCount;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationDiscrepancyRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationItemRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.reconciliation.repository.RunSummaryTotals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus.ACCEPTED;
import static br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus.ADJUSTED;
import static br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus.OPEN;
import static br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus.WRITTEN_OFF;
import static br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType.BANK_AMOUNT_MISMATCH;
import static br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType.FEE_DIVERGENCE;
import static br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType.MISSING_SETTLEMENT;
import static br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType.ORPHAN_SETTLEMENT;
import static br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult.DIVERGENT;
import static br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult.MATCHED;
import static org.assertj.core.api.Assertions.assertThat;

/*
 * Fixture (sale date, result, transaction, expected net, settled net, bank):
 *   S1  06-01 MATCHED    100.00  97.20  97.20  97.20
 *   S2  06-01 DIVERGENT   50.55  48.99  48.00  48.00  FEE_DIVERGENCE OPEN      -> above contract 0.99
 *   S5  06-02 DIVERGENT    5.00   4.80   4.70   4.70  BANK_AMOUNT_MISMATCH OPEN (no fee divergence: 0.10 not counted)
 *   S3  06-03 DIVERGENT   10.10   9.81   null   null  MISSING_SETTLEMENT OPEN
 *   S4  06-03 DIVERGENT   20.00  19.40  19.50  19.50  FEE_DIVERGENCE ACCEPTED  -> above contract max(0, -0.10) = 0
 *   O1  none  DIVERGENT    null   null  30.33  30.33  ORPHAN_SETTLEMENT OPEN
 * Another run of the same merchant holds an OPEN fee divergence that must not leak in.
 */
class PeriodSummaryQueriesIntegrationTest extends AbstractIntegrationTest {

    @Autowired IMerchantRepository merchants;
    @Autowired IReconciliationRunRepository runs;
    @Autowired IReconciliationItemRepository items;
    @Autowired IReconciliationDiscrepancyRepository discrepancies;

    private UUID runId;
    private UUID emptyRunId;
    private MerchantEntity merchant;

    @BeforeEach
    void seed() {
        merchant = new MerchantEntity();
        merchant.setName("Summary queries merchant");
        merchant.setDocument(UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        merchant = merchants.saveAndFlush(merchant);

        ReconciliationRunEntity run = run(merchant, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3));
        runId = run.getId();
        item(run, "S1", "2026-06-01", MATCHED, "100.00", "97.20", "97.20", "97.20");
        item(run, "S2", "2026-06-01", DIVERGENT, "50.55", "48.99", "48.00", "48.00", FEE_DIVERGENCE, OPEN);
        item(run, "S5", "2026-06-02", DIVERGENT, "5.00", "4.80", "4.70", "4.70", BANK_AMOUNT_MISMATCH, OPEN);
        item(run, "S3", "2026-06-03", DIVERGENT, "10.10", "9.81", null, null, MISSING_SETTLEMENT, OPEN);
        item(run, "S4", "2026-06-03", DIVERGENT, "20.00", "19.40", "19.50", "19.50", FEE_DIVERGENCE, ACCEPTED);
        item(run, "O1", null, DIVERGENT, null, null, "30.33", "30.33", ORPHAN_SETTLEMENT, OPEN);

        ReconciliationRunEntity other = run(merchant, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3));
        item(other, "X1", "2026-05-01", DIVERGENT, "999.00", "900.00", "800.00", "800.00", FEE_DIVERGENCE, OPEN);

        emptyRunId = run(merchant, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 3)).getId();
    }

    @Test
    void runTotalsMatchTheFixtureToTheCent() {
        RunSummaryTotals totals = items.sumRunTotals(runId);

        assertThat(totals.grossAmount()).isEqualByComparingTo("185.65");
        assertThat(totals.expectedNetAmount()).isEqualByComparingTo("180.20");
        assertThat(totals.feeAmount()).isEqualByComparingTo("5.45");
        // Orphan O1 (30.33) has no sale date, so it stays out like in the daily series.
        assertThat(totals.bankReceivedAmount()).isEqualByComparingTo("169.40");
    }

    @Test
    void feeAboveContractCountsOnlyShortfallsOfFeeDivergentItems() {
        assertThat(items.sumFeeAboveContract(runId)).isEqualByComparingTo("0.99");
    }

    @Test
    void dailySeriesGroupsBySaleDateAndLeavesOrphansOut() {
        assertThat(items.sumByTransactionDate(runId)).satisfiesExactly(
                day -> assertDay(day, "2026-06-01", "146.19", "145.20", "145.20", 2, 1),
                day -> assertDay(day, "2026-06-02", "4.80", "4.70", "4.70", 1, 0),
                day -> assertDay(day, "2026-06-03", "29.21", "19.50", "19.50", 2, 0));
    }

    @Test
    void openByTypeCountsOnlyOpenDiscrepanciesOfTheRequestedRun() {
        assertThat(discrepancies.countOpenByType(runId)).containsExactlyInAnyOrder(
                new DiscrepancyTypeCount(FEE_DIVERGENCE, 1),
                new DiscrepancyTypeCount(BANK_AMOUNT_MISMATCH, 1),
                new DiscrepancyTypeCount(MISSING_SETTLEMENT, 1),
                new DiscrepancyTypeCount(ORPHAN_SETTLEMENT, 1));
    }

    @Test
    void resolvedCountsEveryNonOpenStatusOfTheRequestedRunOnly() {
        ReconciliationRunEntity resolvedRun = run(merchant, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3));
        item(resolvedRun, "A1", "2026-07-01", DIVERGENT, "1.00", "1.00", "0.90", "0.90", FEE_DIVERGENCE, ACCEPTED);
        item(resolvedRun, "A2", "2026-07-01", DIVERGENT, "1.00", "1.00", "0.90", "0.90", FEE_DIVERGENCE, ADJUSTED);
        item(resolvedRun, "A3", "2026-07-02", DIVERGENT, "1.00", "1.00", "0.90", "0.90", FEE_DIVERGENCE, WRITTEN_OFF);
        item(resolvedRun, "A4", "2026-07-02", DIVERGENT, "1.00", "1.00", "0.90", "0.90", FEE_DIVERGENCE, OPEN);
        item(resolvedRun, "A5", "2026-07-03", DIVERGENT, "1.00", "1.00", null, null, MISSING_SETTLEMENT, OPEN);

        assertThat(discrepancies.countResolved(resolvedRun.getId())).isEqualTo(3);
        // The fixture run holds one ACCEPTED (S4); the other run's OPEN discrepancy does not count.
        assertThat(discrepancies.countResolved(runId)).isEqualTo(1);
        assertThat(discrepancies.countResolved(emptyRunId)).isZero();
    }

    @Test
    void runWithoutItemsHasNoSumsNoDaysAndNoOpenTypes() {
        RunSummaryTotals totals = items.sumRunTotals(emptyRunId);

        assertThat(totals.grossAmount()).isNull();
        assertThat(totals.expectedNetAmount()).isNull();
        assertThat(totals.feeAmount()).isNull();
        assertThat(totals.bankReceivedAmount()).isNull();
        assertThat(items.sumFeeAboveContract(emptyRunId)).isNull();
        assertThat(items.sumByTransactionDate(emptyRunId)).isEmpty();
        assertThat(discrepancies.countOpenByType(emptyRunId)).isEmpty();
    }

    private static void assertDay(
            DailySummaryRow day,
            String date,
            String expected,
            String settled,
            String bank,
            long itemCount,
            long matchedCount) {
        assertThat(day.date()).isEqualTo(LocalDate.parse(date));
        assertThat(day.expectedNetAmount()).isEqualByComparingTo(expected);
        assertThat(day.settledNetAmount()).isEqualByComparingTo(settled);
        assertThat(day.bankReceivedAmount()).isEqualByComparingTo(bank);
        assertThat(day.itemCount()).isEqualTo(itemCount);
        assertThat(day.matchedCount()).isEqualTo(matchedCount);
    }

    private ReconciliationRunEntity run(MerchantEntity merchant, LocalDate fromDate, LocalDate toDate) {
        ReconciliationRunEntity run = new ReconciliationRunEntity();
        run.setMerchant(merchant);
        run.setFromDate(fromDate);
        run.setToDate(toDate);
        run.setStatus(ReconciliationRunStatus.COMPLETED);
        run.setTotalItems(0);
        run.setMatchedCount(0);
        run.setDivergentCount(0);
        return runs.saveAndFlush(run);
    }

    private void item(
            ReconciliationRunEntity run,
            String reference,
            String saleDate,
            ReconciliationResult result,
            String transaction,
            String expectedNet,
            String settledNet,
            String bank) {
        items.saveAndFlush(build(run, reference, saleDate, result, transaction, expectedNet, settledNet, bank));
    }

    private void item(
            ReconciliationRunEntity run,
            String reference,
            String saleDate,
            ReconciliationResult result,
            String transaction,
            String expectedNet,
            String settledNet,
            String bank,
            DiscrepancyType type,
            DiscrepancyStatus status) {
        ReconciliationItemEntity item = build(run, reference, saleDate, result, transaction, expectedNet, settledNet, bank);
        ReconciliationDiscrepancyEntity discrepancy = new ReconciliationDiscrepancyEntity();
        discrepancy.setType(type);
        discrepancy.setStatus(status);
        item.addDiscrepancy(discrepancy);
        items.saveAndFlush(item);
    }

    private static ReconciliationItemEntity build(
            ReconciliationRunEntity run,
            String reference,
            String saleDate,
            ReconciliationResult result,
            String transaction,
            String expectedNet,
            String settledNet,
            String bank) {
        ReconciliationItemEntity item = new ReconciliationItemEntity();
        item.setReconciliationRun(run);
        item.setExternalReference(reference);
        item.setResult(result);
        item.setTransactionDate(saleDate == null ? null : LocalDate.parse(saleDate));
        item.setTransactionAmount(money(transaction));
        item.setExpectedNetAmount(money(expectedNet));
        item.setSettlementNetAmount(money(settledNet));
        item.setBankAmount(money(bank));
        return item;
    }

    private static BigDecimal money(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
