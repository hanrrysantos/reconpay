package br.com.hanrry.reconpay.auth.dto;

import br.com.hanrry.reconpay.auth.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Usuário autenticado")
public record MeResponseDTO(
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean active,

        @Schema(description = "Expiração da conta de demonstração; nulo para conta comum",
                example = "2026-10-21T12:00:00Z", nullable = true)
        Instant demoExpiresAt
) {
}
