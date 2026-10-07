package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class OpenAmountCalculator {

    public BigDecimal sum(List<ReconciliationDiscrepancyEntity> discrepancies) {
        BigDecimal total = BigDecimal.ZERO;
        for (ReconciliationDiscrepancyEntity discrepancy : discrepancies) {
            if (discrepancy.getStatus() != DiscrepancyStatus.OPEN) {
                continue;
            }
            total = total.add(amount(discrepancy));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal amount(ReconciliationDiscrepancyEntity discrepancy) {
        ReconciliationItemEntity item = discrepancy.getReconciliationItem();
        return switch (discrepancy.getType()) {
            case MISSING_SETTLEMENT -> zero(item.getExpectedNetAmount());
            case ORPHAN_SETTLEMENT, MISSING_BANK_CREDIT -> zero(item.getSettlementNetAmount());
            case INCORRECT_AMOUNT -> difference(item.getTransactionAmount(), item.getSettlementAmount());
            case FEE_DIVERGENCE -> difference(item.getExpectedNetAmount(), item.getSettlementNetAmount());
            case BANK_AMOUNT_MISMATCH -> difference(item.getSettlementNetAmount(), bankAmount(discrepancy));
            case ORPHAN_BANK_CREDIT -> bankAmount(discrepancy);
            case STATUS_MISMATCH, PAYMENT_METHOD_MISMATCH, INSTALLMENTS_MISMATCH, AMBIGUOUS_BANK_MATCH ->
                    new BigDecimal("0.00");
        };
    }

    private BigDecimal difference(BigDecimal left, BigDecimal right) {
        return zero(left).subtract(zero(right)).abs();
    }

    private BigDecimal bankAmount(ReconciliationDiscrepancyEntity discrepancy) {
        String actualValue = discrepancy.getActualValue();
        if (actualValue == null || actualValue.isBlank()) {
            return new BigDecimal("0.00");
        }
        return new BigDecimal(actualValue);
    }

    private BigDecimal zero(BigDecimal amount) {
        return amount == null ? new BigDecimal("0.00") : amount;
    }
}
