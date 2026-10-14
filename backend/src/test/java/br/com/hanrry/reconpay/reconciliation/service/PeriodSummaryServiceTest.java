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
                        new ReconciliationProperties(new BigDecimal("0.00"), 5, 31, true, 2, 50)),
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
        assertThat(summary.days()).containsExactly(
                new Day(FROM, new BigDecimal("146.19"), new BigDecimal("145.20"), new BigDecimal("145.20"), 2, 1),
                new Day(LocalDate.parse("2026-06-02"), new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00"), 0, 0),
                new Day(TO, new BigDecimal("29.21"), new BigDecimal("0.00"), new BigDecimal("0.00"), 2, 0));
        assertThat(summary.previousRunMatchRate()).isEqualTo(new BigDecimal("0.7500"));
        assertThat(summary.previousWindowGrossAmount()).isEqualTo(new BigDecimal("50.50"));
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
        assertThat(summary.days()).allSatisfy(day -> assertThat(day.bankReceivedAmount()).isNull());
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
