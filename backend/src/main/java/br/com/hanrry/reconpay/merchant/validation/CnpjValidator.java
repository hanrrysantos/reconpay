package br.com.hanrry.reconpay.merchant.validation;

import br.com.hanrry.reconpay.shared.CnpjRules;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidator implements ConstraintValidator<ValidCnpj, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return CnpjRules.isValid(value);
    }
}
