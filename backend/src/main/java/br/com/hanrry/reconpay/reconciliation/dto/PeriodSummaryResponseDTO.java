package br.com.hanrry.reconpay.reconciliation.dto;

import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PeriodSummaryResponseDTO(
        UUID runId,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal grossAmount,
        BigDecimal expectedNetAmount,
        BigDecimal feeAmount,
        BigDecimal feeAboveContractAmount,
        BigDecimal bankReceivedAmount,
        Map<DiscrepancyType, Long> openByType,
        List<Day> days,
        BigDecimal previousRunMatchRate,
        BigDecimal previousWindowGrossAmount
) {

    public record Day(
            LocalDate date,
            BigDecimal expectedNetAmount,
            BigDecimal settledNetAmount,
            BigDecimal bankReceivedAmount,
            long itemCount,
            long matchedCount
    ) {
    }
}
