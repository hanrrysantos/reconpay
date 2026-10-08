package br.com.hanrry.reconpay.auth.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record MerchantAccessRequestDTO(
        @NotNull(message = "merchantIds é obrigatório")
        List<UUID> merchantIds
) {
}
