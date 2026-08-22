package br.com.hanrry.reconpay.auth.dto;

import java.util.List;
import java.util.UUID;

public record MerchantAccessResponseDTO(
        UUID userId,
        List<UUID> merchantIds
) {
}
