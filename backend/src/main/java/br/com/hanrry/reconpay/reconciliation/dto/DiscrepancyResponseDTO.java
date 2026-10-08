package br.com.hanrry.reconpay.reconciliation.dto;

import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;

import java.util.UUID;

public record DiscrepancyResponseDTO(
        UUID id,
        DiscrepancyStatus status,
        DiscrepancyType type,
        String expectedValue,
        String actualValue
) {
}
