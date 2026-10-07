package br.com.hanrry.reconpay.bankstatement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BankStatementLineResponseDTO(
        UUID id,
        String lineReference,
        String externalReference,
        BigDecimal amount,
        LocalDate movementDate,
        UUID importId
) {
}
