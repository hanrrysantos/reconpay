package br.com.hanrry.reconpay.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The suite shares one database and never cleans up, so every merchant created
 * through the API needs its own document, and that document must be a CNPJ
 * whose check digits match.
 */
public final class TestCnpj {

    private static final int[] FIRST_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private TestCnpj() {
    }

    public static String random() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int[] digits = new int[14];
        do {
            for (int i = 0; i < 12; i++) {
                digits[i] = random.nextInt(10);
            }
        } while (allEqual(digits));

        digits[12] = checkDigit(digits, FIRST_WEIGHTS);
        digits[13] = checkDigit(digits, SECOND_WEIGHTS);

        StringBuilder document = new StringBuilder(14);
        for (int digit : digits) {
            document.append(digit);
        }
        return document.toString();
    }

    private static boolean allEqual(int[] digits) {
        for (int i = 1; i < 12; i++) {
            if (digits[i] != digits[0]) {
                return false;
            }
        }
        return true;
    }

    private static int checkDigit(int[] digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += digits[i] * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
