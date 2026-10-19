package br.com.hanrry.reconpay.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "E-mail da conta que aguarda confirmação")
public record ResendVerificationRequestDTO(
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @Schema(description = "E-mail usado no cadastro", example = "usuario.novo@reconpay.local")
        String email
) {
}
