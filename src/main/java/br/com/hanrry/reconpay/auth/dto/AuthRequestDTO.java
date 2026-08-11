package br.com.hanrry.reconpay.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais para autenticação")
public record AuthRequestDTO(

        @Schema(description = "Email cadastrado no sistema", example = "admin@gmail.com")
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @Schema(description = "Senha do usuário", example = "Admin@123")
        @NotBlank(message = "Senha é obrigatória")
        String password
) {
}
