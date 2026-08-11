package br.com.hanrry.reconpay.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Dados para auto-cadastro de usuário")
public record UserRequestDTO(

        @Schema(description = "Nome completo do usuário", example = "User 1")
        @NotBlank(message = "Nome é obrigatório")
        String name,

        @Schema(description = "Email único no sistema", example = "user@gmail.com")
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @Schema(
                description = "Senha com no mínimo 8 caracteres, uma letra maiúscula e um número",
                example = "User@123"
        )
        @NotBlank(message = "Senha é obrigatória")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Senha deve ter no mínimo 8 caracteres, uma letra maiúscula e um número"
        )
        String password
) {
}
