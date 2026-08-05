package br.com.hanrry.reconpay.feerule.dto;

import br.com.hanrry.reconpay.shared.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FeeRuleResponseDTO (
        UUID id,
        UUID merchantId,
        String merchantName,
        PaymentMethod paymentMethod,
        Integer installments,
        BigDecimal feePercentage,
        BigDecimal fixedFee,
        boolean active,
        Instant createdAt
){
}
