package br.com.hanrry.reconpay.shared;

import java.util.regex.Pattern;

public final class CnpjRules {

    private static final Pattern PUNCTUATION = Pattern.compile("[./\\-\\s]");
    private static final Pattern FOURTEEN_DIGITS = Pattern.compile("\\d{14}");
    private static final int[] FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CnpjRules() {
    }

    /** Removes CNPJ punctuation ({@code .}, {@code /}, {@code -} and whitespace); any other character stays. */
    public static String digits(String value) {
        return PUNCTUATION.matcher(value).replaceAll("");
    }

    /** True only for 14 digits, not all equal, with both check digits right, once the punctuation is removed. */
    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }

        String digits = digits(value);
        if (!FOURTEEN_DIGITS.matcher(digits).matches() || digits.chars().distinct().count() == 1) {
            return false;
        }

        return checkDigit(digits, FIRST_WEIGHTS) == digits.charAt(12) - '0'
                && checkDigit(digits, SECOND_WEIGHTS) == digits.charAt(13) - '0';
    }

    private static int checkDigit(String digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
