package br.com.hanrry.reconpay.externalsettlement.dto;

import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

public record ExternalSettlementResponseDTO(
        UUID id,
        UUID merchantId,
        UUID importId,
        String externalReference,
        BigDecimal amount,
        BigDecimal netAmount,
        PaymentMethod paymentMethod,
        Integer installments,
        TransactionStatus status,
        LocalDate settlementDate,
        Instant createdAt,
        Instant updatedAt
) {
}
