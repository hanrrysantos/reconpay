package br.com.hanrry.reconpay.feeRule.dto;

import br.com.hanrry.reconpay.feeRule.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        LocalDateTime createdAt
){
}
