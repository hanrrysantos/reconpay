package br.com.hanrry.reconpay.feerule.service;

import br.com.hanrry.reconpay.exception.InvalidTransactionAmountException;
import br.com.hanrry.reconpay.feerule.entity.FeeRuleEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpectedNetAmountCalculatorTest {

    @ParameterizedTest
    @CsvSource({
            "100.00, 2.9900, 0.30, 96.71",
            // 3.3333% of 10.00 = 0.33333, kept as 0.3333 before the subtraction.
            "10.00, 3.3333, 0.00, 9.67",
            // 1.5% of 1.00 = 0.0150; 0.985 rounds HALF_UP to 0.99 (HALF_EVEN would give 0.98).
            "1.00, 1.5000, 0.00, 0.99",
            "250.00, 0.0000, 0.00, 250.00"
    })
    void subtractsThePercentageAndTheFixedFeeWithHalfUpRounding(
            String amount, String percentage, String fixedFee, String expectedNet) {
        BigDecimal net = ExpectedNetAmountCalculator.calculate(new BigDecimal(amount), rule(percentage, fixedFee));

        assertThat(net).isEqualByComparingTo(expectedNet);
        assertThat(net.scale()).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({"0.30, 0.0000, 0.30", "0.10, 0.0000, 0.30", "1.00, 100.0000, 0.00"})
    void rejectsAZeroOrNegativeExpectedNet(String amount, String percentage, String fixedFee) {
        assertThatThrownBy(() -> ExpectedNetAmountCalculator.calculate(new BigDecimal(amount), rule(percentage, fixedFee)))
                .isInstanceOf(InvalidTransactionAmountException.class)
                .hasMessage("Valor líquido esperado deve ser maior que zero");
    }

    @Test
    void keepsTheSmallestPositiveNet() {
        assertThat(ExpectedNetAmountCalculator.calculate(new BigDecimal("0.31"), rule("0.0000", "0.30")))
                .isEqualByComparingTo("0.01");
    }

    private static FeeRuleEntity rule(String percentage, String fixedFee) {
        FeeRuleEntity rule = new FeeRuleEntity();
        rule.setFeePercentage(new BigDecimal(percentage));
        rule.setFixedFee(new BigDecimal(fixedFee));
        return rule;
    }
}
