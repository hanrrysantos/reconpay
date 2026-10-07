package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.bankstatement.entity.BankStatementLineEntity;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BankStatementMatcher {

    private final ReconciliationProperties properties;

    public List<ReconciliationItemEntity> apply(
            List<ReconciliationItemEntity> items,
            List<BankStatementLineEntity> lines,
            Set<String> transactionReferencesOutsideWindow) {
        List<ReconciliationItemEntity> result = new ArrayList<>(items);
        Map<String, ReconciliationItemEntity> itemsByReference = new HashMap<>();
        Map<String, ReconciliationItemEntity> settlementsByReference = new HashMap<>();
        for (ReconciliationItemEntity item : result) {
            itemsByReference.put(item.getExternalReference(), item);
            if (item.getExternalSettlement() != null) {
                settlementsByReference.put(item.getExternalReference(), item);
            }
        }

        List<BankStatementLineEntity> considered = lines.stream()
                .filter(line -> !ignored(line, settlementsByReference, transactionReferencesOutsideWindow))
                .toList();

        Set<ReconciliationItemEntity> linkedSettlements = new HashSet<>();
        Set<BankStatementLineEntity> linkedLines = new HashSet<>();
        linkByReference(considered, settlementsByReference, linkedSettlements, linkedLines);

        List<ReconciliationItemEntity> openSettlements = result.stream()
                .filter(item -> item.getExternalSettlement() != null)
                .filter(item -> !linkedSettlements.contains(item))
                .toList();
        List<BankStatementLineEntity> openLines = considered.stream()
                .filter(line -> line.getExternalReference() == null)
                .filter(line -> !linkedLines.contains(line))
                .toList();

        linkByDateAndAmount(result, itemsByReference, openSettlements, openLines);

        for (BankStatementLineEntity line : considered) {
            if (line.getExternalReference() != null && !linkedLines.contains(line)) {
                attachUnpairedLine(
                        result,
                        itemsByReference,
                        line,
                        DiscrepancyType.ORPHAN_BANK_CREDIT,
                        null,
                        formatAmount(line.getAmount()));
            }
        }

        for (ReconciliationItemEntity item : result) {
            item.setResult(item.getDiscrepancies().isEmpty()
                    ? ReconciliationResult.MATCHED
                    : ReconciliationResult.DIVERGENT);
        }
        return result;
    }

    private void linkByReference(
            List<BankStatementLineEntity> lines,
            Map<String, ReconciliationItemEntity> settlementsByReference,
            Set<ReconciliationItemEntity> linkedSettlements,
            Set<BankStatementLineEntity> linkedLines) {
        for (BankStatementLineEntity line : lines) {
            if (line.getExternalReference() == null) {
                continue;
            }
            ReconciliationItemEntity settlement = settlementsByReference.get(line.getExternalReference());
            if (settlement == null) {
                continue;
            }
            linkedSettlements.add(settlement);
            linkedLines.add(line);
            if (!withinTolerance(line.getAmount(), settlement.getSettlementNetAmount())) {
                addDiscrepancy(
                        settlement,
                        DiscrepancyType.BANK_AMOUNT_MISMATCH,
                        formatAmount(settlement.getSettlementNetAmount()),
                        formatAmount(line.getAmount()));
            }
        }
    }

    private void linkByDateAndAmount(
            List<ReconciliationItemEntity> result,
            Map<String, ReconciliationItemEntity> itemsByReference,
            List<ReconciliationItemEntity> openSettlements,
            List<BankStatementLineEntity> openLines) {
        Map<ReconciliationItemEntity, List<BankStatementLineEntity>> linesBySettlement = new HashMap<>();
        Map<BankStatementLineEntity, List<ReconciliationItemEntity>> settlementsByLine = new HashMap<>();
        for (ReconciliationItemEntity settlement : openSettlements) {
            linesBySettlement.put(settlement, new ArrayList<>());
        }
        for (BankStatementLineEntity line : openLines) {
            settlementsByLine.put(line, new ArrayList<>());
        }
        for (ReconciliationItemEntity settlement : openSettlements) {
            for (BankStatementLineEntity line : openLines) {
                if (eligible(line, settlement)) {
                    linesBySettlement.get(settlement).add(line);
                    settlementsByLine.get(line).add(settlement);
                }
            }
        }

        Set<BankStatementLineEntity> pairedLines = new HashSet<>();
        for (ReconciliationItemEntity settlement : openSettlements) {
            List<BankStatementLineEntity> eligibleLines = linesBySettlement.get(settlement);
            if (eligibleLines.size() == 1
                    && settlementsByLine.get(eligibleLines.getFirst()).size() == 1) {
                pairedLines.add(eligibleLines.getFirst());
                continue;
            }
            if (eligibleLines.isEmpty()) {
                addDiscrepancy(
                        settlement,
                        DiscrepancyType.MISSING_BANK_CREDIT,
                        formatAmount(settlement.getSettlementNetAmount()),
                        null);
            } else {
                addDiscrepancy(
                        settlement,
                        DiscrepancyType.AMBIGUOUS_BANK_MATCH,
                        formatAmount(settlement.getSettlementNetAmount()),
                        null);
            }
        }

        for (BankStatementLineEntity line : openLines) {
            if (pairedLines.contains(line)) {
                continue;
            }
            List<ReconciliationItemEntity> eligibleSettlements = settlementsByLine.get(line);
            DiscrepancyType type = eligibleSettlements.isEmpty()
                    ? DiscrepancyType.ORPHAN_BANK_CREDIT
                    : DiscrepancyType.AMBIGUOUS_BANK_MATCH;
            attachUnpairedLine(
                    result,
                    itemsByReference,
                    line,
                    type,
                    null,
                    formatAmount(line.getAmount()));
        }
    }

    private boolean ignored(
            BankStatementLineEntity line,
            Map<String, ReconciliationItemEntity> settlementsByReference,
            Set<String> transactionReferencesOutsideWindow) {
        String code = line.getExternalReference();
        if (code == null || settlementsByReference.containsKey(code)) {
            return false;
        }
        return transactionReferencesOutsideWindow.contains(code);
    }

    private boolean eligible(BankStatementLineEntity line, ReconciliationItemEntity settlement) {
        return Objects.equals(line.getMovementDate(), settlement.getSettlementDate())
                && withinTolerance(line.getAmount(), settlement.getSettlementNetAmount());
    }

    private boolean withinTolerance(BigDecimal bankAmount, BigDecimal netAmount) {
        return bankAmount.subtract(netAmount).abs().compareTo(properties.amountTolerance()) <= 0;
    }

    private void attachUnpairedLine(
            List<ReconciliationItemEntity> result,
            Map<String, ReconciliationItemEntity> itemsByReference,
            BankStatementLineEntity line,
            DiscrepancyType type,
            String expectedValue,
            String actualValue) {
        ReconciliationItemEntity existing = itemsByReference.get(line.getLineReference());
        if (existing != null) {
            if (existing.getExternalSettlement() != null) {
                addDiscrepancy(existing, type, expectedValue, actualValue);
            }
            return;
        }

        ReconciliationItemEntity created = new ReconciliationItemEntity();
        created.setExternalReference(line.getLineReference());
        addDiscrepancy(created, type, expectedValue, actualValue);
        result.add(created);
        itemsByReference.put(line.getLineReference(), created);
    }

    private void addDiscrepancy(
            ReconciliationItemEntity item,
            DiscrepancyType type,
            String expectedValue,
            String actualValue) {
        ReconciliationDiscrepancyEntity discrepancy = new ReconciliationDiscrepancyEntity();
        discrepancy.setType(type);
        discrepancy.setExpectedValue(expectedValue);
        discrepancy.setActualValue(actualValue);
        discrepancy.setStatus(DiscrepancyStatus.OPEN);
        item.addDiscrepancy(discrepancy);
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
