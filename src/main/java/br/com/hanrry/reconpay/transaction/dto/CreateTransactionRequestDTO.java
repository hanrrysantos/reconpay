package br.com.hanrry.reconpay.transaction.dto;

import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequestDTO(
        @NotBlank
        @Size(max = 100)
        String externalReference,

        @NotNull
        @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
        BigDecimal amount,

        @NotNull
        PaymentMethod paymentMethod,

        @NotNull
        @Min(value = 1, message = "Número de parcelas deve ser no mínimo 1")
        Integer installments,

        @NotNull
        @PastOrPresent(message = "Data da transação não pode ser futura")
        LocalDate transactionDate
) {
}
