package br.com.hanrry.reconpay.reconciliation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Janela de datas da conciliação, aplicada sobre a data da transação")
public record RunReconciliationRequestDTO(

        @Schema(description = "Início da janela, inclusivo", example = "2026-07-01")
        @NotNull(message = "fromDate é obrigatório")
        LocalDate fromDate,

        @Schema(description = "Fim da janela, inclusivo", example = "2026-07-31")
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
