package br.com.hanrry.reconpay.auth.dto;

import br.com.hanrry.reconpay.auth.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponseDTO (
        UUID id,
        String name,
        String email,
        UserRole role,
        boolean active,
        LocalDateTime createdAt
){
}
