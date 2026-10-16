package br.com.hanrry.reconpay.transaction.dto;

import java.time.Instant;
import java.util.UUID;

public record TransactionImportResponseDTO(
        UUID id,
        UUID merchantId,
        String fileName,
        Integer totalRows,
        Instant createdAt
) {
}
