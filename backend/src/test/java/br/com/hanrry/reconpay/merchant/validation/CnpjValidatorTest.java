package br.com.hanrry.reconpay.merchant.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CnpjValidatorTest {

    private final CnpjValidator validator = new CnpjValidator();

    @Test
    void leavesNullAndBlankToNotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void acceptsValidCnpjWithOrWithoutPunctuation() {
        assertThat(validator.isValid("12345678000195", null)).isTrue();
        assertThat(validator.isValid("12.345.678/0001-95", null)).isTrue();
    }

    @Test
    void rejectsInvalidCnpj() {
        assertThat(validator.isValid("12345678000199", null)).isFalse();
        assertThat(validator.isValid("1234567800019", null)).isFalse();
        assertThat(validator.isValid("11111111111111", null)).isFalse();
        assertThat(validator.isValid("12A45678000195", null)).isFalse();
    }
}
