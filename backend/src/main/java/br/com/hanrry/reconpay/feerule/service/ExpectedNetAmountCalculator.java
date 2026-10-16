package br.com.hanrry.reconpay.feerule.service;

import br.com.hanrry.reconpay.exception.InvalidTransactionAmountException;
import br.com.hanrry.reconpay.feerule.entity.FeeRuleEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;

/*
 * The single place that turns a sale amount and a fee rule into the expected net amount,
 * shared by the single create and the CSV import so both always agree.
 */
public final class ExpectedNetAmountCalculator {

    private ExpectedNetAmountCalculator() {
    }

    public static BigDecimal calculate(BigDecimal amount, FeeRuleEntity feeRule) {
        BigDecimal percentageFee = amount
                .multiply(feeRule.getFeePercentage())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal net = amount
                .subtract(percentageFee)
                .subtract(feeRule.getFixedFee())
                .setScale(2, RoundingMode.HALF_UP);
        if (net.signum() <= 0) {
            throw new InvalidTransactionAmountException(
                    "Valor líquido esperado deve ser maior que zero");
        }
        return net;
    }
}
