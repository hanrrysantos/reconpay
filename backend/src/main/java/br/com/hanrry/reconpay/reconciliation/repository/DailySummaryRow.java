package br.com.hanrry.reconpay.reconciliation.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySummaryRow(
        LocalDate date,
        BigDecimal expectedNetAmount,
        BigDecimal settledNetAmount,
        BigDecimal bankReceivedAmount,
        long itemCount,
        long matchedCount
) {
}
