package br.com.hanrry.reconpay.feeRule.dto;

import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FeeRuleRequestDTO(
        @NotNull
        PaymentMethod paymentMethod,

        @NotNull
        @Min(1)
        Integer installments,

        @NotNull
        @DecimalMin("0.0000")
        BigDecimal feePercentage,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal fixedFee
) {
}
