package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class ReconciliationEngine {

    public List<ReconciliationItemEntity> reconcile(
            Map<String, InternalTransactionEntity> transactionsByReference,
            Map<String, ExternalSettlementEntity> settlementsByReference) {
        Set<String> allReferences = new HashSet<>();
        allReferences.addAll(transactionsByReference.keySet());
        allReferences.addAll(settlementsByReference.keySet());

        return allReferences.stream()
                .sorted()
                .map(reference -> buildItem(
                        reference,
                        transactionsByReference.get(reference),
                        settlementsByReference.get(reference)))
                .toList();
    }

    private ReconciliationItemEntity buildItem(
            String externalReference,
            InternalTransactionEntity transaction,
            ExternalSettlementEntity settlement) {
        ReconciliationItemEntity item = new ReconciliationItemEntity();
        item.setExternalReference(externalReference);
        item.setInternalTransaction(transaction);
        item.setExternalSettlement(settlement);

        List<ReconciliationDiscrepancyEntity> discrepancies = detectDiscrepancies(transaction, settlement);
        discrepancies.forEach(item::addDiscrepancy);

        item.setResult(discrepancies.isEmpty() ? ReconciliationResult.MATCHED : ReconciliationResult.DIVERGENT);
        return item;
    }

    private List<ReconciliationDiscrepancyEntity> detectDiscrepancies(
            InternalTransactionEntity transaction,
            ExternalSettlementEntity settlement) {
        if (transaction == null) {
            return List.of(discrepancy(
                    DiscrepancyType.ORPHAN_SETTLEMENT,
                    null,
                    settlement.getExternalReference()));
        }

        if (settlement == null) {
            return List.of(discrepancy(
                    DiscrepancyType.MISSING_SETTLEMENT,
                    transaction.getExternalReference(),
                    null));
        }

        List<ReconciliationDiscrepancyEntity> discrepancies = new ArrayList<>();

        if (transaction.getAmount().compareTo(settlement.getAmount()) != 0) {
            discrepancies.add(discrepancy(
                    DiscrepancyType.INCORRECT_AMOUNT,
                    formatAmount(transaction.getAmount()),
                    formatAmount(settlement.getAmount())));
        } else if (transaction.getExpectedNetAmount().compareTo(settlement.getNetAmount()) != 0) {
            discrepancies.add(discrepancy(
                    DiscrepancyType.FEE_DIVERGENCE,
                    formatAmount(transaction.getExpectedNetAmount()),
                    formatAmount(settlement.getNetAmount())));
        }

        if (transaction.getStatus() != settlement.getStatus()) {
            discrepancies.add(discrepancy(
                    DiscrepancyType.STATUS_MISMATCH,
                    transaction.getStatus().name(),
                    settlement.getStatus().name()));
        }

        if (transaction.getPaymentMethod() != settlement.getPaymentMethod()) {
            discrepancies.add(discrepancy(
                    DiscrepancyType.PAYMENT_METHOD_MISMATCH,
                    transaction.getPaymentMethod().name(),
                    settlement.getPaymentMethod().name()));
        }

        if (!transaction.getInstallments().equals(settlement.getInstallments())) {
            discrepancies.add(discrepancy(
                    DiscrepancyType.INSTALLMENTS_MISMATCH,
                    transaction.getInstallments().toString(),
                    settlement.getInstallments().toString()));
        }

        return discrepancies;
    }

    private ReconciliationDiscrepancyEntity discrepancy(
            DiscrepancyType type,
            String expectedValue,
            String actualValue) {
        ReconciliationDiscrepancyEntity entity = new ReconciliationDiscrepancyEntity();
        entity.setType(type);
        entity.setExpectedValue(expectedValue);
        entity.setActualValue(actualValue);
        return entity;
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2).toPlainString();
    }
}
