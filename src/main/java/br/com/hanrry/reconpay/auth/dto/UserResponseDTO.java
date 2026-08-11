package br.com.hanrry.reconpay.auth.dto;

import br.com.hanrry.reconpay.auth.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Dados públicos de um usuário")
public record UserResponseDTO (

        @Schema(description = "Identificador único do usuário", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Nome do usuário", example = "Hanrry Santos")
        String name,

        @Schema(description = "Email do usuário", example = "hanrry@gmail.com")
        String email,

        @Schema(description = "Perfil de acesso", example = "FINANCIAL_ANALYST")
        UserRole role,

        @Schema(description = "Indica se a conta está ativa", example = "true")
        boolean active,

        @Schema(description = "Data de criação da conta (UTC)", example = "2026-08-10T12:00:00Z")
        Instant createdAt
){
}
