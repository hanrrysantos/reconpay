package br.com.hanrry.reconpay.merchant.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MerchantResponseDTO(
        UUID id,
        String name,
        String document,
        boolean active,
        LocalDateTime createdAt

){
}
