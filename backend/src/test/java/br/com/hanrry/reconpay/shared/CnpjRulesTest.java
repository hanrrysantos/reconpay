package br.com.hanrry.reconpay.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CnpjRulesTest {

    @Test
    void digitsShouldRemoveCnpjPunctuation() {
        assertThat(CnpjRules.digits("12.345.678/0001-95")).isEqualTo("12345678000195");
        assertThat(CnpjRules.digits(" 12 345 678 0001 95 ")).isEqualTo("12345678000195");
    }

    @Test
    void digitsShouldKeepCharactersThatAreNotCnpjPunctuation() {
        assertThat(CnpjRules.digits("12A45678000195")).isEqualTo("12A45678000195");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678000195", "12.345.678/0001-95", "11111111000191", "11.111.111/0001-91"})
    void isValidShouldAcceptCnpjWithMatchingCheckDigits(String value) {
        assertThat(CnpjRules.isValid(value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1234567800019",      // 13 dígitos
            "123456780001950",    // 15 dígitos
            "12345678000185",     // primeiro verificador errado
            "12345678000194",     // segundo verificador errado
            "11111111111111",
            "00000000000000",
            "12A45678000195",
            "12345678_000195",
            ""
    })
    void isValidShouldRejectAnythingElse(String value) {
        assertThat(CnpjRules.isValid(value)).isFalse();
    }

    @Test
    void isValidShouldRejectNull() {
        assertThat(CnpjRules.isValid(null)).isFalse();
    }
}
