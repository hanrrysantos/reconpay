package br.com.hanrry.reconpay.transaction.dto;

import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponseDTO(
        UUID id,
        UUID merchantId,
        String externalReference,
        BigDecimal amount,
        BigDecimal expectedNetAmount,
        PaymentMethod paymentMethod,
        Integer installments,
        TransactionStatus status,
        LocalDate transactionDate,
        Instant createdAt,
        Instant updatedAt
) {
}
