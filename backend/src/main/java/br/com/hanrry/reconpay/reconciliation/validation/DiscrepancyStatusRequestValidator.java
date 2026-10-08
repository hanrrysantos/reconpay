package br.com.hanrry.reconpay.reconciliation.validation;

import br.com.hanrry.reconpay.reconciliation.dto.UpdateDiscrepancyStatusRequestDTO;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class DiscrepancyStatusRequestValidator
        implements ConstraintValidator<ValidDiscrepancyStatusRequest, UpdateDiscrepancyStatusRequestDTO> {

    @Override
    public boolean isValid(UpdateDiscrepancyStatusRequestDTO value, ConstraintValidatorContext context) {
        if (value == null || value.status() == null) {
            return true;
        }

        BigDecimal correctionAmount = value.correctionAmount();

        if (value.status() == DiscrepancyStatus.ADJUSTED) {
            if (correctionAmount == null) {
                return false;
            }
            return correctionAmount.compareTo(BigDecimal.ZERO) != 0;
        }

        if (correctionAmount != null) {
            return value.status() != DiscrepancyStatus.ACCEPTED
                    && value.status() != DiscrepancyStatus.WRITTEN_OFF
                    && value.status() != DiscrepancyStatus.OPEN;
        }

        return true;
    }
}
