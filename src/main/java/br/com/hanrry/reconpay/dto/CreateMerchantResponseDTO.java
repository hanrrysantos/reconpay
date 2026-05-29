package br.com.hanrry.reconpay.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateMerchantResponseDTO (
        UUID id,
        String name,
        String document,
        boolean active,
        LocalDateTime createdAt
){
}
