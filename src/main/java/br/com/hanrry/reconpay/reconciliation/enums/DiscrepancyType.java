package br.com.hanrry.reconpay.reconciliation.enums;

public enum DiscrepancyType {
    MISSING_SETTLEMENT,
    ORPHAN_SETTLEMENT,
    INCORRECT_AMOUNT,
    FEE_DIVERGENCE,
    STATUS_MISMATCH,
    PAYMENT_METHOD_MISMATCH,
    INSTALLMENTS_MISMATCH
}
