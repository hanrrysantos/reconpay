package br.com.hanrry.reconpay.reconciliation.dto;

import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DiscrepancyDetailResponseDTO(
        UUID id,
        DiscrepancyType type,
        String expectedValue,
        String actualValue,
        DiscrepancyStatus status,
        List<Adjustment> adjustments,
        List<Transition> transitions
) {

    public record Adjustment(
            BigDecimal amount,
            boolean voided
    ) {
    }

    public record Transition(
            UUID actorUserId,
            DiscrepancyStatus fromStatus,
            DiscrepancyStatus toStatus,
            String note,
            Instant createdAt
    ) {
    }
}
