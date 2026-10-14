package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.reconciliation.dto.PeriodSummaryResponseDTO;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.DailySummaryRow;
import br.com.hanrry.reconpay.reconciliation.repository.DiscrepancyTypeCount;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationDiscrepancyRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationItemRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.reconciliation.repository.RunSummaryTotals;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PeriodSummaryService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final PeriodWindowPolicy windowPolicy;
    private final IMerchantRepository merchantRepository;
    private final IReconciliationRunRepository runRepository;
    private final IReconciliationItemRepository itemRepository;
    private final IReconciliationDiscrepancyRepository discrepancyRepository;

    @Transactional(readOnly = true)
    public PeriodSummaryResponseDTO summarize(UUID merchantId, LocalDate fromDate, LocalDate toDate) {
        windowPolicy.ensureWindow(fromDate, toDate);
        merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));
        windowPolicy.rejectInFlight(merchantId, fromDate, toDate);
        ReconciliationRunEntity run = windowPolicy.currentCompleted(merchantId, fromDate, toDate);

        // Runs finished before the matcher recorded bank credits have no bank data, not zero.
        boolean bankTracked = run.isBankTracked();
        RunSummaryTotals totals = itemRepository.sumRunTotals(run.getId());

        return new PeriodSummaryResponseDTO(
                run.getId(),
                fromDate,
                toDate,
                money(totals.grossAmount()),
                money(totals.expectedNetAmount()),
                money(totals.feeAmount()),
                money(itemRepository.sumFeeAboveContract(run.getId())),
                bankTracked ? money(totals.bankReceivedAmount()) : null,
                openByType(run.getId()),
                days(run.getId(), fromDate, toDate, bankTracked),
                previousRunMatchRate(merchantId, fromDate, toDate),
                previousWindowGrossAmount(merchantId, fromDate, toDate));
    }

    private Map<DiscrepancyType, Long> openByType(UUID runId) {
        Map<DiscrepancyType, Long> counts = new EnumMap<>(DiscrepancyType.class);
        for (DiscrepancyTypeCount count : discrepancyRepository.countOpenByType(runId)) {
            counts.put(count.type(), count.count());
        }
        return counts;
    }

    /* One entry per date of the window, so a day without sales reads as zero instead of missing. */
    private List<PeriodSummaryResponseDTO.Day> days(
            UUID runId, LocalDate fromDate, LocalDate toDate, boolean bankTracked) {
        Map<LocalDate, DailySummaryRow> rows = itemRepository.sumByTransactionDate(runId).stream()
                .collect(Collectors.toMap(DailySummaryRow::date, Function.identity()));
        List<PeriodSummaryResponseDTO.Day> days = new ArrayList<>();
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            DailySummaryRow row = rows.get(date);
            if (row == null) {
                days.add(new PeriodSummaryResponseDTO.Day(date, ZERO, ZERO, bankTracked ? ZERO : null, 0, 0));
            } else {
                days.add(new PeriodSummaryResponseDTO.Day(
                        date,
                        money(row.expectedNetAmount()),
                        money(row.settledNetAmount()),
                        bankTracked ? money(row.bankReceivedAmount()) : null,
                        row.itemCount(),
                        row.matchedCount()));
            }
        }
        return days;
    }

    private BigDecimal previousRunMatchRate(UUID merchantId, LocalDate fromDate, LocalDate toDate) {
        return runRepository
                .findFirstByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNotNullOrderBySupersededAtDescCreatedAtDesc(
                        merchantId, fromDate, toDate, ReconciliationRunStatus.COMPLETED)
                .map(PeriodSummaryService::matchRate)
                .orElse(null);
    }

    /* A full calendar month compares with the whole previous month; any other window with the equal-length window before it. */
    private BigDecimal previousWindowGrossAmount(UUID merchantId, LocalDate fromDate, LocalDate toDate) {
        LocalDate previousFrom;
        if (isFullCalendarMonth(fromDate, toDate)) {
            previousFrom = fromDate.minusMonths(1);
        } else {
            previousFrom = fromDate.minusDays(ChronoUnit.DAYS.between(fromDate, toDate) + 1);
        }
        return runRepository
                .findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                        merchantId, previousFrom, fromDate.minusDays(1),
                        ReconciliationRunStatus.COMPLETED)
                .map(previous -> money(itemRepository.sumRunTotals(previous.getId()).grossAmount()))
                .orElse(null);
    }

    private static boolean isFullCalendarMonth(LocalDate fromDate, LocalDate toDate) {
        return fromDate.getDayOfMonth() == 1 && toDate.equals(fromDate.withDayOfMonth(fromDate.lengthOfMonth()));
    }

    /* Same rule as the period response: no items means no rate. */
    private static BigDecimal matchRate(ReconciliationRunEntity run) {
        if (run.getTotalItems() == 0) {
            return null;
        }
        return BigDecimal.valueOf(run.getMatchedCount())
                .divide(BigDecimal.valueOf(run.getTotalItems()), 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(2, RoundingMode.HALF_UP);
    }
}
