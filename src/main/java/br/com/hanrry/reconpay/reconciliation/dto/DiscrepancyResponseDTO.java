package br.com.hanrry.reconpay.reconciliation.dto;

import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;

public record DiscrepancyResponseDTO(
        DiscrepancyType type,
        String expectedValue,
        String actualValue
) {
}
