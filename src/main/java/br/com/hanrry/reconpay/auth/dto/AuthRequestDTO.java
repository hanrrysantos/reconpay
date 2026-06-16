package br.com.hanrry.reconpay.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequestDTO(

        @NotBlank
        String email,

        @NotBlank
        String password
) {
}
