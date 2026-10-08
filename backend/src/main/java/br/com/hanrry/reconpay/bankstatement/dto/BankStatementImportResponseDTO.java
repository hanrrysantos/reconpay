package br.com.hanrry.reconpay.bankstatement.dto;

import java.time.Instant;
import java.util.UUID;

public record BankStatementImportResponseDTO(
        UUID id,
        UUID merchantId,
        String fileName,
        Integer totalRows,
        Instant createdAt
) {
}
