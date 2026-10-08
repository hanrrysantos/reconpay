package br.com.hanrry.reconpay.feerule.dto;

import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdateFeeRuleRequestDTO(
        PaymentMethod paymentMethod,

        @Min(value = 1, message = "Número de parcelas deve ser no mínimo 1")
        Integer installments,

        @DecimalMin(value = "0.0000", message = "Percentual de taxa deve ser maior ou igual a zero")
        @DecimalMax("100.0000")
        @Digits(integer = 3, fraction = 4)
        BigDecimal feePercentage,

        @DecimalMin(value = "0.00", message = "Taxa fixa deve ser maior ou igual a zero")
        @Digits(integer = 17, fraction = 2)
        BigDecimal fixedFee
) {
}
