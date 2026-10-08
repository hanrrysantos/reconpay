package br.com.hanrry.reconpay.feerule.dto;

import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
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
        @DecimalMax("100.0000")
        @Digits(integer = 3, fraction = 4)
        BigDecimal feePercentage,

        @NotNull
        @DecimalMin("0.00")
        @Digits(integer = 17, fraction = 2)
        BigDecimal fixedFee
) {
}
