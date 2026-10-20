package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.exception.InvalidReconciliationWindowException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.PeriodConflictException;
import br.com.hanrry.reconpay.exception.PeriodNotFoundException;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.reconciliation.dto.PeriodSummaryResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.PeriodSummaryResponseDTO.Day;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.DailySummaryRow;
import br.com.hanrry.reconpay.reconciliation.repository.DiscrepancyTypeCount;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationDiscrepancyRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationItemRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.reconciliation.repository.RunSummaryTotals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodSummaryServiceTest {

    private static final LocalDate FROM = LocalDate.parse("2026-06-01");
    private static final LocalDate TO = LocalDate.parse("2026-06-03");
    private static final LocalDate PREVIOUS_FROM = LocalDate.parse("2026-05-29");
    private static final LocalDate PREVIOUS_TO = LocalDate.parse("2026-05-31");
    private static final List<ReconciliationRunStatus> IN_FLIGHT = List.of(
            ReconciliationRunStatus.PENDING,
            ReconciliationRunStatus.RUNNING);

    @Mock IMerchantRepository merchantRepository;
    @Mock IReconciliationRunRepository runRepository;
    @Mock IReconciliationItemRepository itemRepository;
    @Mock IReconciliationDiscrepancyRepository discrepancyRepository;

    private PeriodSummaryService service;
    private UUID merchantId;
    private ReconciliationRunEntity run;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        service = new PeriodSummaryService(
                new PeriodWindowPolicy(
                        runRepository,
                        new ReconciliationProperties(new BigDecimal("0.00"), 5, 31, true, 2, 50, 300_000)),
                merchantRepository,
                runRepository,
                itemRepository,
                discrepancyRepository);
        run = run(FROM, TO, 5, 1);
        run.setBankTracked(true);
    }

    @Test
    void buildsEveryFieldOfTheContractWithTwoDecimalMoney() {
        stubReadableWindow();
        when(itemRepository.sumRunTotals(run.getId())).thenReturn(new RunSummaryTotals(
                new BigDecimal("185.65"), new BigDecimal("180.20"), new BigDecimal("5.45"), new BigDecimal("199.7")));
        when(itemRepository.sumFeeAboveContract(run.getId())).thenReturn(new BigDecimal("0.99"));
        when(itemRepository.sumByTransactionDate(run.getId())).thenReturn(List.of(
                new DailySummaryRow(FROM, new BigDecimal("146.19"), new BigDecimal("145.20"), new BigDecimal("145.20"), 2, 1),
                new DailySummaryRow(TO, new BigDecimal("29.21"), null, null, 2, 0)));
        when(discrepancyRepository.countOpenByType(run.getId())).thenReturn(List.of(
                new DiscrepancyTypeCount(DiscrepancyType.MISSING_SETTLEMENT, 1),
                new DiscrepancyTypeCount(DiscrepancyType.FEE_DIVERGENCE, 3)));
        when(discrepancyRepository.countResolved(run.getId())).thenReturn(4L);
        ReconciliationRunEntity superseded = run(FROM, TO, 4, 3);
        when(runRepository
                .findFirstByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNotNullOrderBySupersededAtDescCreatedAtDesc(
                        merchantId, FROM, TO, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.of(superseded));
        ReconciliationRunEntity previousWindow = run(PREVIOUS_FROM, PREVIOUS_TO, 2, 2);
        when(runRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, PREVIOUS_FROM, PREVIOUS_TO, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.of(previousWindow));
        when(itemRepository.sumRunTotals(previousWindow.getId())).thenReturn(new RunSummaryTotals(
                new BigDecimal("50.5"), new BigDecimal("49.00"), new BigDecimal("1.50"), new BigDecimal("49.00")));

        PeriodSummaryResponseDTO summary = service.summarize(merchantId, FROM, TO);

        assertThat(summary.runId()).isEqualTo(run.getId());
        assertThat(summary.fromDate()).isEqualTo(FROM);
        assertThat(summary.toDate()).isEqualTo(TO);
        assertThat(summary.grossAmount()).isEqualTo(new BigDecimal("185.65"));
        assertThat(summary.expectedNetAmount()).isEqualTo(new BigDecimal("180.20"));
        assertThat(summary.feeAmount()).isEqualTo(new BigDecimal("5.45"));
        assertThat(summary.feeAboveContractAmount()).isEqualTo(new BigDecimal("0.99"));
        assertThat(summary.bankReceivedAmount()).isEqualTo(new BigDecimal("199.70"));
        assertThat(summary.openByType()).containsExactlyInAnyOrderEntriesOf(Map.of(
                DiscrepancyType.MISSING_SETTLEMENT, 1L,
                DiscrepancyType.FEE_DIVERGENCE, 3L));
        assertThat(summary.resolvedCount()).isEqualTo(4);
        assertThat(summary.days()).containsExactly(
                new Day(FROM, new BigDecimal("146.19"), new BigDecimal("145.20"), new BigDecimal("145.20"),
                        new BigDecimal("0.99"), 2, 1),
                new Day(LocalDate.parse("2026-06-02"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"),
                        new BigDecimal("0.00"), 0, 0),
                new Day(TO, new BigDecimal("29.21"), new BigDecimal("0.00"), new BigDecimal("0.00"),
                        new BigDecimal("29.21"), 2, 0));
        assertThat(summary.previousRunMatchRate()).isEqualTo(new BigDecimal("0.7500"));
        assertThat(summary.previousWindowGrossAmount()).isEqualTo(new BigDecimal("50.50"));
        // 5.45 / 185.65 = 2.9356%; (185.65 − 50.50) / 50.50 = 267.6238%
        assertThat(summary.feePercent()).isEqualTo(new BigDecimal("2.94"));
        assertThat(summary.previousWindowGrossChangePercent()).isEqualTo(new BigDecimal("267.6"));
    }

    @Test
    void runWithoutBankTrackingReturnsBankFieldsAsNull() {
        run.setBankTracked(false);
        stubReadableWindow();
        when(itemRepository.sumRunTotals(run.getId())).thenReturn(new RunSummaryTotals(
                new BigDecimal("10.00"), new BigDecimal("9.70"), new BigDecimal("0.30"), null));
        when(itemRepository.sumByTransactionDate(run.getId())).thenReturn(List.of(
                new DailySummaryRow(FROM, new BigDecimal("9.70"), new BigDecimal("9.70"), null, 1, 1)));

        PeriodSummaryResponseDTO summary = service.summarize(merchantId, FROM, TO);

        assertThat(summary.bankReceivedAmount()).isNull();
        assertThat(summary.days()).hasSize(3);
        assertThat(summary.days()).allSatisfy(day -> {
            assertThat(day.bankReceivedAmount()).isNull();
            assertThat(day.bankShortfallAmount()).isNull();
        });
        assertThat(summary.days().getFirst().settledNetAmount()).isEqualTo(new BigDecimal("9.70"));
    }

    @Test
    void runWithoutItemsReturnsZeroMoneyAndAZeroEntryForEveryDay() {
        stubReadableWindow();
        when(itemRepository.sumRunTotals(run.getId())).thenReturn(new RunSummaryTotals(null, null, null, null));

        PeriodSummaryResponseDTO summary = service.summarize(merchantId, FROM, TO);

        assertThat(summary.grossAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.expectedNetAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.feeAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.feeAboveContractAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.bankReceivedAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.openByType()).isEmpty();
        assertThat(summary.days()).extracting(Day::date).containsExactly(
                FROM, LocalDate.parse("2026-06-02"), TO);
        assertThat(summary.days()).allSatisfy(day -> {
            assertThat(day.expectedNetAmount()).isEqualTo(new BigDecimal("0.00"));
            assertThat(day.settledNetAmount()).isEqualTo(new BigDecimal("0.00"));
            assertThat(day.bankReceivedAmount()).isEqualTo(new BigDecimal("0.00"));
            assertThat(day.bankShortfallAmount()).isEqualTo(new BigDecimal("0.00"));
            assertThat(day.itemCount()).isZero();
            assertThat(day.matchedCount()).isZero();
        });
    }

    @Test
    void comparisonsAreNullWithoutPreviousRunOrPreviousWindow() {
        stubReadableWindow();
        when(itemRepository.sumRunTotals(run.getId())).thenReturn(new RunSummaryTotals(null, null, null, null));

        PeriodSummaryResponseDTO summary = service.summarize(merchantId, FROM, TO);

        assertThat(summary.previousRunMatchRate()).isNull();
        assertThat(summary.previousWindowGrossAmount()).isNull();
        assertThat(summary.previousWindowGrossChangePercent()).isNull();
    }

    @Test
    void previousRunWithoutItemsHasNoRateAndEmptyPreviousWindowGrossIsZero() {
        stubReadableWindow();
        when(itemRepository.sumRunTotals(run.getId())).thenReturn(new RunSummaryTotals(null, null, null, null));
        when(runRepository
                .findFirstByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNotNullOrderBySupersededAtDescCreatedAtDesc(
                        merchantId, FROM, TO, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.of(run(FROM, TO, 0, 0)));
        ReconciliationRunEntity previousWindow = run(PREVIOUS_FROM, PREVIOUS_TO, 0, 0);
        when(runRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, PREVIOUS_FROM, PREVIOUS_TO, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.of(previousWindow));
        when(itemRepository.sumRunTotals(previousWindow.getId()))
                .thenReturn(new RunSummaryTotals(null, null, null, null));

        PeriodSummaryResponseDTO summary = service.summarize(merchantId, FROM, TO);

        assertThat(summary.previousRunMatchRate()).isNull();
        assertThat(summary.previousWindowGrossAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(summary.previousWindowGrossChangePercent()).as("no change over a zero base").isNull();
        assertThat(summary.feePercent()).as("no fee share of zero gross").isNull();
    }

    @Test
    void dailyShortfallIsExpectedMinusBankAndNegativeWhenTheBankReceivedMore() {
        stubReadableWindow();
        when(itemRepository.sumRunTotals(run.getId())).thenReturn(new RunSummaryTotals(null, null, null, null));
        when(itemRepository.sumByTransactionDate(run.getId())).thenReturn(List.of(
                new DailySummaryRow(FROM, new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("80.00"), 1, 0),
                new DailySummaryRow(TO, new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("120.00"), 1, 0)));

        PeriodSummaryResponseDTO summary = service.summarize(merchantId, FROM, TO);

        assertThat(summary.days()).extracting(Day::bankShortfallAmount).containsExactly(
                new BigDecimal("20.00"), new BigDecimal("0.00"), new BigDecimal("-20.00"));
    }

    @Test
    void feePercentRoundsHalfUpToTwoPlaces() {
        stubReadableWindow();
        when(itemRepository.sumRunTotals(run.getId())).thenReturn(new RunSummaryTotals(
                new BigDecimal("1000.00"), new BigDecimal("969.65"), new BigDecimal("30.35"), null));

        assertThat(service.summarize(merchantId, FROM, TO).feePercent()).isEqualTo(new BigDecimal("3.04"));
    }

    @Test
    void grossChangePercentRoundsHalfUpToOnePlaceInBothDirections() {
        assertThat(grossChangeFor("1062.00", "1000.00")).isEqualTo(new BigDecimal("6.2"));
        assertThat(grossChangeFor("937.50", "1000.00")).isEqualTo(new BigDecimal("-6.3"));
    }

    @Test
    void fullCalendarMonthComparesWithTheWholePreviousCalendarMonth() {
        assertThat(previousGrossFor("2026-09-01", "2026-09-30", "2026-08-01", "2026-08-31", "1234.5"))
                .isEqualTo(new BigDecimal("1234.50"));
    }

    @Test
    void marchComparesWithTheWholeFebruaryOfACommonYear() {
        assertThat(previousGrossFor("2026-03-01", "2026-03-31", "2026-02-01", "2026-02-28", "80"))
                .isEqualTo(new BigDecimal("80.00"));
    }

    @Test
    void marchComparesWithTheWholeFebruaryOfALeapYear() {
        assertThat(previousGrossFor("2028-03-01", "2028-03-31", "2028-02-01", "2028-02-29", "81"))
                .isEqualTo(new BigDecimal("81.00"));
    }

    @Test
    void windowThatIsNotACalendarMonthComparesWithTheEqualLengthWindowBefore() {
        assertThat(previousGrossFor("2026-09-10", "2026-09-19", "2026-08-31", "2026-09-09", "42.1"))
                .isEqualTo(new BigDecimal("42.10"));
    }

    @Test
    void windowFromDayOneThatEndsBeforeTheLastDayKeepsTheEqualLengthRule() {
        assertThat(previousGrossFor("2026-09-01", "2026-09-29", "2026-08-03", "2026-08-31", "7"))
                .isEqualTo(new BigDecimal("7.00"));
    }

    @Test
    void fullCalendarMonthWithoutCurrentRunInThePreviousMonthHasNoComparison() {
        assertThat(previousGrossFor("2026-09-01", "2026-09-30", "2026-08-01", "2026-08-31", null)).isNull();
    }

    @Test
    void previousRunMatchRateRoundsHalfUpToFourPlaces() {
        stubReadableWindow();
        when(itemRepository.sumRunTotals(run.getId())).thenReturn(new RunSummaryTotals(null, null, null, null));
        when(runRepository
                .findFirstByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNotNullOrderBySupersededAtDescCreatedAtDesc(
                        merchantId, FROM, TO, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.of(run(FROM, TO, 3, 2)));

        assertThat(service.summarize(merchantId, FROM, TO).previousRunMatchRate())
                .isEqualTo(new BigDecimal("0.6667"));
    }

    @Test
    void invertedWindowIsRejectedBeforeAnyLookup() {
        assertThatThrownBy(() -> service.summarize(merchantId, TO, FROM))
                .isInstanceOf(InvalidReconciliationWindowException.class);
        verifyNoInteractions(merchantRepository, runRepository, itemRepository, discrepancyRepository);
    }

    @Test
    void windowBeyondMaxDaysIsRejected() {
        assertThatThrownBy(() -> service.summarize(merchantId, FROM, FROM.plusDays(31)))
                .isInstanceOf(InvalidReconciliationWindowException.class);
    }

    @Test
    void inactiveMerchantIsNotFound() {
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.summarize(merchantId, FROM, TO))
                .isInstanceOf(MerchantNotFoundException.class);
    }

    @Test
    void runInFlightIsAConflict() {
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(new MerchantEntity()));
        when(runRepository.existsByMerchant_IdAndFromDateAndToDateAndStatusIn(merchantId, FROM, TO, IN_FLIGHT))
                .thenReturn(true);

        assertThatThrownBy(() -> service.summarize(merchantId, FROM, TO))
                .isInstanceOf(PeriodConflictException.class);
        verifyNoInteractions(itemRepository, discrepancyRepository);
    }

    @Test
    void windowWithoutCurrentCompletedRunIsNotFound() {
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(new MerchantEntity()));
        when(runRepository.existsByMerchant_IdAndFromDateAndToDateAndStatusIn(merchantId, FROM, TO, IN_FLIGHT))
                .thenReturn(false);
        when(runRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, FROM, TO, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.summarize(merchantId, FROM, TO))
                .isInstanceOf(PeriodNotFoundException.class);
        verifyNoInteractions(itemRepository, discrepancyRepository);
    }

    private void stubReadableWindow() {
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(new MerchantEntity()));
        when(runRepository.existsByMerchant_IdAndFromDateAndToDateAndStatusIn(merchantId, FROM, TO, IN_FLIGHT))
                .thenReturn(false);
        when(runRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, FROM, TO, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.of(run));
        // Lenient: tests that have a previous window replace this answer.
        lenient().when(runRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, PREVIOUS_FROM, PREVIOUS_TO, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.empty());
    }

    /* Stubs only the expected previous window: a lookup of any other window fails under strict stubs. */
    private BigDecimal previousGrossFor(
            String from, String to, String previousFrom, String previousTo, String previousGross) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        ReconciliationRunEntity current = run(fromDate, toDate, 0, 0);
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(new MerchantEntity()));
        when(runRepository.existsByMerchant_IdAndFromDateAndToDateAndStatusIn(merchantId, fromDate, toDate, IN_FLIGHT))
                .thenReturn(false);
        when(runRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, fromDate, toDate, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.of(current));
        when(itemRepository.sumRunTotals(current.getId())).thenReturn(new RunSummaryTotals(null, null, null, null));
        LocalDate previousFromDate = LocalDate.parse(previousFrom);
        LocalDate previousToDate = LocalDate.parse(previousTo);
        if (previousGross == null) {
            when(runRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                    merchantId, previousFromDate, previousToDate, ReconciliationRunStatus.COMPLETED))
                    .thenReturn(Optional.empty());
        } else {
            ReconciliationRunEntity previous = run(previousFromDate, previousToDate, 1, 1);
            when(runRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                    merchantId, previousFromDate, previousToDate, ReconciliationRunStatus.COMPLETED))
                    .thenReturn(Optional.of(previous));
            when(itemRepository.sumRunTotals(previous.getId())).thenReturn(new RunSummaryTotals(
                    new BigDecimal(previousGross), null, null, null));
        }
        return service.summarize(merchantId, fromDate, toDate).previousWindowGrossAmount();
    }

    private BigDecimal grossChangeFor(String gross, String previousGross) {
        LocalDate fromDate = LocalDate.parse("2026-09-01");
        LocalDate toDate = LocalDate.parse("2026-09-30");
        ReconciliationRunEntity current = run(fromDate, toDate, 0, 0);
        ReconciliationRunEntity previous = run(LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-31"), 1, 1);
        lenient().when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(new MerchantEntity()));
        lenient().when(runRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, fromDate, toDate, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.of(current));
        lenient().when(runRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, previous.getFromDate(), previous.getToDate(), ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.of(previous));
        when(itemRepository.sumRunTotals(current.getId())).thenReturn(new RunSummaryTotals(
                new BigDecimal(gross), null, null, null));
        when(itemRepository.sumRunTotals(previous.getId())).thenReturn(new RunSummaryTotals(
                new BigDecimal(previousGross), null, null, null));
        return service.summarize(merchantId, fromDate, toDate).previousWindowGrossChangePercent();
    }

    private static ReconciliationRunEntity run(LocalDate fromDate, LocalDate toDate, int total, int matched) {
        ReconciliationRunEntity run = new ReconciliationRunEntity();
        run.setId(UUID.randomUUID());
        run.setFromDate(fromDate);
        run.setToDate(toDate);
        run.setStatus(ReconciliationRunStatus.COMPLETED);
        run.setTotalItems(total);
        run.setMatchedCount(matched);
        run.setDivergentCount(total - matched);
        return run;
    }
}
