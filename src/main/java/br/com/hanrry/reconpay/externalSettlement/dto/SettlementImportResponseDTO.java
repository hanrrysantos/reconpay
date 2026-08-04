package br.com.hanrry.reconpay.externalSettlement.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SettlementImportResponseDTO(
        UUID id,
        UUID merchantId,
        String fileName,
        Integer totalRows,
        LocalDateTime createdAt
) {
}
