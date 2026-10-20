package br.com.hanrry.reconpay.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;
import java.util.random.RandomGenerator;

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

    @Test
    void generateShouldReturnValidCnpjForEverySeed() {
        for (long seed = 0; seed < 1000; seed++) {
            String cnpj = CnpjRules.generate(new Random(seed));

            assertThat(cnpj).as("seed %d", seed).matches("\\d{14}");
            assertThat(CnpjRules.isValid(cnpj)).as("seed %d: %s", seed, cnpj).isTrue();
        }
    }

    @Test
    void generateShouldReturnSameCnpjForSameSeed() {
        assertThat(CnpjRules.generate(new Random(42))).isEqualTo(CnpjRules.generate(new Random(42)));
    }

    @Test
    void generateShouldNotReturnRepeatedDigitsEvenWhenTheGeneratorRepeats() {
        // Um gerador que só devolve 0 produziria "00000000000000" se todos os dígitos viessem dele.
        RandomGenerator zeros = () -> 0L;

        String cnpj = CnpjRules.generate(zeros);

        assertThat(cnpj.chars().distinct().count()).isGreaterThan(1);
        assertThat(CnpjRules.isValid(cnpj)).isTrue();
    }
}
