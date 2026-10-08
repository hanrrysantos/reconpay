package br.com.hanrry.reconpay.externalsettlement.dto;

public record ImportRowErrorDTO(
        int row,
        String message
) {
}
