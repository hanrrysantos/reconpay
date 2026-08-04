package br.com.hanrry.reconpay.externalSettlement.dto;

public record ImportRowErrorDTO(
        int row,
        String message
) {
}
