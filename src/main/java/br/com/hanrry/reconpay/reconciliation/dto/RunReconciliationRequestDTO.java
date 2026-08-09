package br.com.hanrry.reconpay.reconciliation.dto;

import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDate;

public record RunReconciliationRequestDTO(
        LocalDate fromDate,
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
