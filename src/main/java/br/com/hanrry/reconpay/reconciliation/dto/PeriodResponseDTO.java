package br.com.hanrry.reconpay.reconciliation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PeriodResponseDTO(
        UUID runId,
        LocalDate fromDate,
        LocalDate toDate,
        int totalItems,
        int matchedCount,
        int divergentCount,
        BigDecimal matchRate,
        BigDecimal openAmount,
        boolean locked,
        Instant lockedAt,
        Long closeDurationSeconds
) {
}
