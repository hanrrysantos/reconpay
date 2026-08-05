package br.com.hanrry.reconpay.merchant.dto;

import java.time.Instant;
import java.util.UUID;

public record MerchantResponseDTO(
        UUID id,
        String name,
        String document,
        boolean active,
        Instant createdAt

){
}
