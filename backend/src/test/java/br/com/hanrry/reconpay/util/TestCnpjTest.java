package br.com.hanrry.reconpay.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TestCnpjTest {

    @Test
    void shouldGenerateCnpjsWithMatchingCheckDigits() {
        for (int sample = 0; sample < 1000; sample++) {
            String document = TestCnpj.random();

            assertThat(document).matches("\\d{14}");
            assertThat(document.chars().distinct().count()).isGreaterThan(1);
            assertThat(document.substring(12))
                    .as("check digits of %s", document)
                    .isEqualTo(expectedCheckDigits(document.substring(0, 12)));
        }
    }

    @Test
    void oracleShouldMatchKnownValidCnpjs() {
        assertThat(expectedCheckDigits("123456780001")).isEqualTo("95");
        assertThat(expectedCheckDigits("111111110001")).isEqualTo("91");
    }

    // Written out step by step so the oracle does not share code with the generator.
    private static String expectedCheckDigits(String base) {
        String first = String.valueOf(mod11(base, "543298765432"));
        String second = String.valueOf(mod11(base + first, "6543298765432"));
        return first + second;
    }

    private static int mod11(String digits, String weights) {
        int sum = 0;
        for (int i = 0; i < weights.length(); i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * Character.getNumericValue(weights.charAt(i));
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
