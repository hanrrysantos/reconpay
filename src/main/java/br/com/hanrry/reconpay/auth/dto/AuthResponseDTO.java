package br.com.hanrry.reconpay.auth.dto;

public record AuthResponseDTO (
        String token,
        String type,
        long expiresIn
){
    public AuthResponseDTO(String token){
        this(token, "Bearer", 86400);
    }
}
