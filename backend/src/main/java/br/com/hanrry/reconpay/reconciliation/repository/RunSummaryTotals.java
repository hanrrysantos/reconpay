package br.com.hanrry.reconpay.reconciliation.repository;

import java.math.BigDecimal;

/*
 * Sums over the items of one run. A sum is null when no item has the
 * column, so callers decide what zero means for them.
 */
public record RunSummaryTotals(
        BigDecimal grossAmount,
        BigDecimal expectedNetAmount,
        BigDecimal feeAmount,
        BigDecimal bankReceivedAmount
) {
}
