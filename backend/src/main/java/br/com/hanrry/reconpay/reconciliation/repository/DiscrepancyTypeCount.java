package br.com.hanrry.reconpay.reconciliation.repository;

import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;

public record DiscrepancyTypeCount(DiscrepancyType type, long count) {
}
