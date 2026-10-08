package br.com.hanrry.reconpay.reconciliation.dto;

import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.validation.ValidDiscrepancyStatusRequest;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@ValidDiscrepancyStatusRequest
public record UpdateDiscrepancyStatusRequestDTO(
        @NotNull
        DiscrepancyStatus status,

        @Size(max = 500)
        String note,

        @Digits(integer = 17, fraction = 2)
        BigDecimal correctionAmount
) {
}
