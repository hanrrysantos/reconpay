package br.com.hanrry.reconpay.auth.dto;

import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.shared.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequestDTO(

        @NotBlank(message = "Nome é obrigatório")
        String name,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @ValidPassword
        String password,

        @NotNull(message = "Papel é obrigatório")
        UserRole role
) {
}
