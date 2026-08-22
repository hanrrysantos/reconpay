package br.com.hanrry.reconpay.reconciliation.dto;

import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReconciliationRunResponseDTO(
        UUID id,
        UUID merchantId,
        LocalDate fromDate,
        LocalDate toDate,
        ReconciliationRunStatus status,
        Integer totalItems,
        Integer matchedCount,
        Integer divergentCount,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt,
        String errorMessage,
        Instant supersededAt
) {
}
