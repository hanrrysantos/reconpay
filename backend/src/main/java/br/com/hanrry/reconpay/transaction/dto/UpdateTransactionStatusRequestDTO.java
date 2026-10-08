package br.com.hanrry.reconpay.transaction.dto;

import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTransactionStatusRequestDTO(
        @NotNull
        TransactionStatus status
) {
}
