package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ReconciliationEngine {

    private final ReconciliationProperties properties;

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
        snapshot(item, transaction, settlement);

        List<ReconciliationDiscrepancyEntity> discrepancies = detectDiscrepancies(item);
        discrepancies.forEach(item::addDiscrepancy);

        item.setResult(discrepancies.isEmpty() ? ReconciliationResult.MATCHED : ReconciliationResult.DIVERGENT);
        return item;
    }

    private void snapshot(
            ReconciliationItemEntity item,
            InternalTransactionEntity transaction,
            ExternalSettlementEntity settlement) {
        if (transaction != null) {
            item.setTransactionAmount(transaction.getAmount());
            item.setExpectedNetAmount(transaction.getExpectedNetAmount());
            item.setTransactionPaymentMethod(transaction.getPaymentMethod());
            item.setTransactionInstallments(transaction.getInstallments());
            item.setTransactionStatus(transaction.getStatus());
            item.setTransactionDate(transaction.getTransactionDate());
        }

        if (settlement != null) {
            item.setSettlementAmount(settlement.getAmount());
            item.setSettlementNetAmount(settlement.getNetAmount());
            item.setSettlementPaymentMethod(settlement.getPaymentMethod());
            item.setSettlementInstallments(settlement.getInstallments());
            item.setSettlementStatus(settlement.getStatus());
            item.setSettlementDate(settlement.getSettlementDate());
        }
    }

    private List<ReconciliationDiscrepancyEntity> detectDiscrepancies(ReconciliationItemEntity item) {
        if (item.getInternalTransaction() == null) {
            return List.of(discrepancy(
                    DiscrepancyType.ORPHAN_SETTLEMENT,
                    null,
                    item.getExternalReference()));
        }

        if (item.getExternalSettlement() == null) {
            return List.of(discrepancy(
                    DiscrepancyType.MISSING_SETTLEMENT,
                    item.getExternalReference(),
                    null));
        }

        List<ReconciliationDiscrepancyEntity> discrepancies = new ArrayList<>();

        if (exceedsTolerance(item.getTransactionAmount(), item.getSettlementAmount())) {
            discrepancies.add(discrepancy(
                    DiscrepancyType.INCORRECT_AMOUNT,
                    formatAmount(item.getTransactionAmount()),
                    formatAmount(item.getSettlementAmount())));
        }

        if (exceedsTolerance(item.getExpectedNetAmount(), item.getSettlementNetAmount())) {
            discrepancies.add(discrepancy(
                    DiscrepancyType.FEE_DIVERGENCE,
                    formatAmount(item.getExpectedNetAmount()),
                    formatAmount(item.getSettlementNetAmount())));
        }

        if (item.getTransactionStatus() != item.getSettlementStatus()) {
            discrepancies.add(discrepancy(
                    DiscrepancyType.STATUS_MISMATCH,
                    name(item.getTransactionStatus()),
                    name(item.getSettlementStatus())));
        }

        if (item.getTransactionPaymentMethod() != item.getSettlementPaymentMethod()) {
            discrepancies.add(discrepancy(
                    DiscrepancyType.PAYMENT_METHOD_MISMATCH,
                    name(item.getTransactionPaymentMethod()),
                    name(item.getSettlementPaymentMethod())));
        }

        if (!Objects.equals(item.getTransactionInstallments(), item.getSettlementInstallments())) {
            discrepancies.add(discrepancy(
                    DiscrepancyType.INSTALLMENTS_MISMATCH,
                    Objects.toString(item.getTransactionInstallments(), null),
                    Objects.toString(item.getSettlementInstallments(), null)));
        }

        return discrepancies;
    }

    private boolean exceedsTolerance(BigDecimal expected, BigDecimal actual) {
        if (expected == null || actual == null) {
            return !(expected == null && actual == null);
        }
        return expected.subtract(actual).abs().compareTo(properties.amountTolerance()) > 0;
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

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
