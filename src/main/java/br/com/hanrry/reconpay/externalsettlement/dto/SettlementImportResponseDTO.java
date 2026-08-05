package br.com.hanrry.reconpay.externalsettlement.dto;

import java.time.Instant;
import java.util.UUID;

public record SettlementImportResponseDTO(
        UUID id,
        UUID merchantId,
        String fileName,
        Integer totalRows,
        Instant createdAt
) {
}
