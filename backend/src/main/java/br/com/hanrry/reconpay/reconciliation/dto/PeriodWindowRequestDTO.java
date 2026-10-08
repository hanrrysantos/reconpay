package br.com.hanrry.reconpay.reconciliation.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PeriodWindowRequestDTO(

        @NotNull(message = "fromDate é obrigatório")
        LocalDate fromDate,

        @NotNull(message = "toDate é obrigatório")
        LocalDate toDate
) {

    @AssertTrue(message = "fromDate deve ser anterior ou igual a toDate")
    public boolean isValidDateRange() {
        if (fromDate == null || toDate == null) {
            return true;
        }
        return !fromDate.isAfter(toDate);
    }
}
