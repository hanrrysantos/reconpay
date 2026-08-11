package br.com.hanrry.reconpay.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token JWT retornado após autenticação bem-sucedida")
public record AuthResponseDTO (

        @Schema(description = "Token JWT para uso no header Authorization", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Tipo do token", example = "Bearer")
        String type,

        @Schema(description = "Tempo de expiração em segundos", example = "86400")
        long expiresIn
){
    public AuthResponseDTO(String token){
        this(token, "Bearer", 86400);
    }
}
