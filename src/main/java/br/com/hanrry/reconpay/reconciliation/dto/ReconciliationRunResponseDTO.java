package br.com.hanrry.reconpay.reconciliation.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReconciliationRunResponseDTO(
        UUID id,
        UUID merchantId,
        LocalDate fromDate,
        LocalDate toDate,
        Integer totalItems,
        Integer matchedCount,
        Integer divergentCount,
        Instant createdAt
) {
}
