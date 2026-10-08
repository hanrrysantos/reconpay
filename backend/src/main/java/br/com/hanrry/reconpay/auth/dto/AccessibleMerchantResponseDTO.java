package br.com.hanrry.reconpay.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Merchant acessível ao usuário autenticado")
public record AccessibleMerchantResponseDTO(
        UUID id,
        String name,
        String document
) {
}
