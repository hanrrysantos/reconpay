package br.com.hanrry.reconpay.feeRule.dto;

import br.com.hanrry.reconpay.feeRule.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdateFeeRuleRequestDTO(
        PaymentMethod paymentMethod,

        @Min(1)
        Integer installments,

        @DecimalMin("0.0000")
        BigDecimal feePercentage,

        @DecimalMin("0.00")
        BigDecimal fixedFee
) {
}
