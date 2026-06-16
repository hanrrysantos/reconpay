package br.com.hanrry.reconpay.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequestDTO(

        @NotBlank(message = "Nome é obrigatório")
        String name
) {
}
