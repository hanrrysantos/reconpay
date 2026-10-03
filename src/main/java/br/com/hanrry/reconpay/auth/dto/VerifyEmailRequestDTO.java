package br.com.hanrry.reconpay.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Token recebido por e-mail para ativação da conta")
public record VerifyEmailRequestDTO(
        @NotBlank(message = "Token é obrigatório")
        @Schema(description = "Token de verificação de e-mail")
        String token
) {
}
