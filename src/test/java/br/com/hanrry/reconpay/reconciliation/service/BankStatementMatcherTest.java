package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.bankstatement.entity.BankStatementLineEntity;
import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BankStatementMatcherTest {

    private static final LocalDate SETTLEMENT_DATE = LocalDate.parse("2026-07-30");
    private static final LocalDate OTHER_DATE = LocalDate.parse("2026-08-02");

    private final ReconciliationEngine reconciliationEngine = engineWithTolerance("0.00");
    private final BankStatementMatcher matcher = matcherWithTolerance("0.00");

    @Test
    void shouldPairByReferenceWhenAmountIsWithinToleranceAndDateDiffers() {
        ReconciliationItemEntity settlement = settlementItem("TXN-1", "145.00", SETTLEMENT_DATE);
        BankStatementLineEntity line = bankLine("L-1", "TXN-1", "145.00", OTHER_DATE);

        List<ReconciliationItemEntity> result = matcher.apply(List.of(settlement), List.of(line), Set.of());

        assertThat(result).containsExactly(settlement);
        assertThat(settlement.getResult()).isEqualTo(ReconciliationResult.MATCHED);
        assertThat(settlement.getDiscrepancies()).isEmpty();
    }

    @Test
    void shouldPairByReferenceWhenDifferenceEqualsTolerance() {
        BankStatementMatcher tolerant = matcherWithTolerance("0.05");
        ReconciliationItemEntity settlement = settlementItem("TXN-1", "100.00", SETTLEMENT_DATE);
        BankStatementLineEntity line = bankLine("L-1", "TXN-1", "100.05", OTHER_DATE);

        List<ReconciliationItemEntity> result = tolerant.apply(List.of(settlement), List.of(line), Set.of());

        assertThat(result).containsExactly(settlement);
        assertThat(settlement.getResult()).isEqualTo(ReconciliationResult.MATCHED);
        assertThat(settlement.getDiscrepancies()).isEmpty();
    }

    @Test
    void shouldRecordBankAmountMismatchAndLeaveTheSettlementOutOfFallback() {
        ReconciliationItemEntity settlement = settlementItem("TXN-1", "10.5", SETTLEMENT_DATE);
        BankStatementLineEntity mismatch = bankLine("L-1", "TXN-1", "10.555", OTHER_DATE);
        BankStatementLineEntity bait = bankLine("L-BAIT", null, "10.50", SETTLEMENT_DATE);

        List<ReconciliationItemEntity> result = matcher.apply(
                List.of(settlement), List.of(mismatch, bait), Set.of());

        assertThat(result).extracting(ReconciliationItemEntity::getExternalReference)
                .containsExactlyInAnyOrder("TXN-1", "L-BAIT");
        assertThat(settlement.getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(settlement.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.BANK_AMOUNT_MISMATCH);
        ReconciliationDiscrepancyEntity mismatchDiscrepancy = discrepancy(
                settlement, DiscrepancyType.BANK_AMOUNT_MISMATCH);
        assertThat(mismatchDiscrepancy.getExpectedValue()).isEqualTo("10.50");
        assertThat(mismatchDiscrepancy.getActualValue()).isEqualTo("10.56");
        assertThat(mismatchDiscrepancy.getStatus()).isEqualTo(DiscrepancyStatus.OPEN);

        ReconciliationItemEntity orphan = item(result, "L-BAIT");
        assertThat(orphan.getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(orphan.getInternalTransaction()).isNull();
        assertThat(orphan.getExternalSettlement()).isNull();
        assertThat(orphan.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.ORPHAN_BANK_CREDIT);
        ReconciliationDiscrepancyEntity orphanDiscrepancy = discrepancy(
                orphan, DiscrepancyType.ORPHAN_BANK_CREDIT);
        assertThat(orphanDiscrepancy.getExpectedValue()).isNull();
        assertThat(orphanDiscrepancy.getActualValue()).isEqualTo("10.50");
        assertThat(orphanDiscrepancy.getStatus()).isEqualTo(DiscrepancyStatus.OPEN);
    }

    @Test
    void shouldPairUniqueFallbackLineWhenDateAndAmountAreWithinTolerance() {
        BankStatementMatcher tolerant = matcherWithTolerance("0.05");
        ReconciliationItemEntity settlement = settlementItem("TXN-1", "100.00", SETTLEMENT_DATE);
        BankStatementLineEntity line = bankLine("L-1", null, "100.05", SETTLEMENT_DATE);

        List<ReconciliationItemEntity> result = tolerant.apply(List.of(settlement), List.of(line), Set.of());

        assertThat(result).containsExactly(settlement);
        assertThat(settlement.getResult()).isEqualTo(ReconciliationResult.MATCHED);
        assertThat(settlement.getDiscrepancies()).isEmpty();
    }

    @Test
    void shouldRecordMissingBankCreditWhenDateDiffers() {
        ReconciliationItemEntity settlement = settlementItem("TXN-1", "10.5", SETTLEMENT_DATE);
        BankStatementLineEntity line = bankLine("L-1", null, "10.5", OTHER_DATE);

        List<ReconciliationItemEntity> result = matcher.apply(List.of(settlement), List.of(line), Set.of());

        assertThat(settlement.getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(settlement.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.MISSING_BANK_CREDIT);
        ReconciliationDiscrepancyEntity missing = discrepancy(settlement, DiscrepancyType.MISSING_BANK_CREDIT);
        assertThat(missing.getExpectedValue()).isEqualTo("10.50");
        assertThat(missing.getActualValue()).isNull();
        assertThat(missing.getStatus()).isEqualTo(DiscrepancyStatus.OPEN);

        ReconciliationItemEntity orphan = item(result, "L-1");
        assertThat(orphan.getExternalReference()).isEqualTo("L-1");
        assertThat(orphan.getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(orphan.getInternalTransaction()).isNull();
        assertThat(orphan.getExternalSettlement()).isNull();
        ReconciliationDiscrepancyEntity orphanDiscrepancy = discrepancy(
                orphan, DiscrepancyType.ORPHAN_BANK_CREDIT);
        assertThat(orphanDiscrepancy.getExpectedValue()).isNull();
        assertThat(orphanDiscrepancy.getActualValue()).isEqualTo("10.50");
        assertThat(orphanDiscrepancy.getStatus()).isEqualTo(DiscrepancyStatus.OPEN);
    }

    @Test
    void shouldRecordMissingBankCreditWhenFallbackAmountExceedsTolerance() {
        BankStatementMatcher tolerant = matcherWithTolerance("0.05");
        ReconciliationItemEntity settlement = settlementItem("TXN-1", "100.00", SETTLEMENT_DATE);
        BankStatementLineEntity line = bankLine("L-1", null, "100.06", SETTLEMENT_DATE);

        List<ReconciliationItemEntity> result = tolerant.apply(List.of(settlement), List.of(line), Set.of());

        assertThat(settlement.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.MISSING_BANK_CREDIT);
        assertThat(discrepancy(settlement, DiscrepancyType.MISSING_BANK_CREDIT).getExpectedValue())
                .isEqualTo("100.00");
        assertThat(discrepancy(settlement, DiscrepancyType.MISSING_BANK_CREDIT).getActualValue()).isNull();
        assertThat(item(result, "L-1").getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.ORPHAN_BANK_CREDIT);
        assertThat(discrepancy(item(result, "L-1"), DiscrepancyType.ORPHAN_BANK_CREDIT).getActualValue())
                .isEqualTo("100.06");
    }

    @Test
    void shouldRecordAmbiguousMatchWhenSeveralLinesAreEligibleAndPairNone() {
        ReconciliationItemEntity settlement = settlementItem("TXN-1", "100.00", SETTLEMENT_DATE);
        BankStatementLineEntity first = bankLine("L-1", null, "100.00", SETTLEMENT_DATE);
        BankStatementLineEntity second = bankLine("L-2", null, "100.00", SETTLEMENT_DATE);

        List<ReconciliationItemEntity> result = matcher.apply(
                List.of(settlement), List.of(first, second), Set.of());

        assertThat(result).extracting(ReconciliationItemEntity::getExternalReference)
                .containsExactlyInAnyOrder("TXN-1", "L-1", "L-2");
        assertThat(settlement.getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(settlement.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.AMBIGUOUS_BANK_MATCH);
        ReconciliationDiscrepancyEntity settlementDiscrepancy = discrepancy(
                settlement, DiscrepancyType.AMBIGUOUS_BANK_MATCH);
        assertThat(settlementDiscrepancy.getExpectedValue()).isEqualTo("100.00");
        assertThat(settlementDiscrepancy.getActualValue()).isNull();
        assertThat(settlementDiscrepancy.getStatus()).isEqualTo(DiscrepancyStatus.OPEN);

        assertAmbiguousBankLine(item(result, "L-1"), "100.00");
        assertAmbiguousBankLine(item(result, "L-2"), "100.00");
    }

    @Test
    void shouldRecordAmbiguousMatchWhenOneLineIsEligibleForTwoSettlements() {
        ReconciliationItemEntity first = settlementItem("TXN-1", "100.00", SETTLEMENT_DATE);
        ReconciliationItemEntity second = settlementItem("TXN-2", "100.00", SETTLEMENT_DATE);
        BankStatementLineEntity line = bankLine("L-1", null, "100.00", SETTLEMENT_DATE);

        List<ReconciliationItemEntity> result = matcher.apply(
                List.of(first, second), List.of(line), Set.of());

        assertThat(result).extracting(ReconciliationItemEntity::getExternalReference)
                .containsExactlyInAnyOrder("TXN-1", "TXN-2", "L-1");
        assertAmbiguousSettlement(first);
        assertAmbiguousSettlement(second);
        assertAmbiguousBankLine(item(result, "L-1"), "100.00");
    }

    @Test
    void shouldRecordOrphanBankCreditWhenCodedLineMatchesNoSettlement() {
        BankStatementLineEntity line = bankLine("L-ORPHAN", "UNKNOWN", "10.5", SETTLEMENT_DATE);

        List<ReconciliationItemEntity> result = matcher.apply(List.of(), List.of(line), Set.of());

        assertThat(result).hasSize(1);
        ReconciliationItemEntity orphan = result.getFirst();
        assertThat(orphan.getExternalReference()).isEqualTo("L-ORPHAN");
        assertThat(orphan.getInternalTransaction()).isNull();
        assertThat(orphan.getExternalSettlement()).isNull();
        assertThat(orphan.getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(orphan.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.ORPHAN_BANK_CREDIT);
        ReconciliationDiscrepancyEntity discrepancy = discrepancy(
                orphan, DiscrepancyType.ORPHAN_BANK_CREDIT);
        assertThat(discrepancy.getExpectedValue()).isNull();
        assertThat(discrepancy.getActualValue()).isEqualTo("10.50");
        assertThat(discrepancy.getStatus()).isEqualTo(DiscrepancyStatus.OPEN);
    }

    @Test
    void shouldIgnoreLineWhoseCodeExistsOnlyOutsideTheWindow() {
        ReconciliationItemEntity settlement = settlementItem("TXN-1", "100.00", SETTLEMENT_DATE);
        BankStatementLineEntity outside = bankLine("L-OUT", "OUTSIDE", "100.00", SETTLEMENT_DATE);

        List<ReconciliationItemEntity> result = matcher.apply(
                List.of(settlement), List.of(outside), Set.of("OUTSIDE"));

        assertThat(result).extracting(ReconciliationItemEntity::getExternalReference)
                .containsExactly("TXN-1");
        assertThat(settlement.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.MISSING_BANK_CREDIT);
    }

    @Test
    void shouldLeaveUnsettledItemFreeOfBankDiscrepancy() {
        List<ReconciliationItemEntity> sales = reconciliationEngine.reconcile(
                Map.of("TXN-SALE", transaction("TXN-SALE")),
                Map.of());
        BankStatementLineEntity line = bankLine("BANK-1", "TXN-SALE", "50.00", SETTLEMENT_DATE);

        List<ReconciliationItemEntity> result = matcher.apply(sales, List.of(line), Set.of());

        ReconciliationItemEntity sale = item(result, "TXN-SALE");
        assertThat(sale.getExternalSettlement()).isNull();
        assertThat(sale.getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(sale.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.MISSING_SETTLEMENT);
        ReconciliationItemEntity orphan = item(result, "BANK-1");
        assertThat(orphan.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.ORPHAN_BANK_CREDIT);
        assertThat(orphan.getExternalSettlement()).isNull();
        assertThat(orphan.getInternalTransaction()).isNull();
    }

    @Test
    void shouldKeepSaleVersusSettlementTypesWhenBankAmountMatches() {
        List<ReconciliationItemEntity> items = reconciliationEngine.reconcile(
                Map.of("TXN-FEE", transaction("TXN-FEE", "150.00", "145.00")),
                Map.of("TXN-FEE", settlement("TXN-FEE", "150.00", "140.00")));
        BankStatementLineEntity line = bankLine("L-1", "TXN-FEE", "140.00", OTHER_DATE);

        List<ReconciliationItemEntity> result = matcher.apply(items, List.of(line), Set.of());

        assertThat(result).containsExactlyElementsOf(items);
        ReconciliationItemEntity item = result.getFirst();
        assertThat(item.getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(item.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.FEE_DIVERGENCE);
        assertThat(discrepancy(item, DiscrepancyType.FEE_DIVERGENCE).getStatus())
                .isEqualTo(DiscrepancyStatus.OPEN);
    }

    @Test
    void shouldMarkEverySettlementMissingBankCreditWhenStatementIsEmpty() {
        List<ReconciliationItemEntity> items = reconciliationEngine.reconcile(
                Map.of(
                        "TXN-A", transaction("TXN-A", "150.00", "145.00"),
                        "TXN-B", transaction("TXN-B", "150.00", "145.00"),
                        "TXN-C", transaction("TXN-C")),
                Map.of(
                        "TXN-A", settlement("TXN-A", "150.00", "145.00"),
                        "TXN-B", settlement("TXN-B", "150.00", "140.00"),
                        "EXT-D", settlement("EXT-D", "80.00", "78.00")));

        List<ReconciliationItemEntity> result = matcher.apply(items, List.of(), Set.of());

        ReconciliationItemEntity paired = item(result, "TXN-A");
        assertThat(paired.getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(paired.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.MISSING_BANK_CREDIT);
        assertThat(discrepancy(paired, DiscrepancyType.MISSING_BANK_CREDIT).getExpectedValue())
                .isEqualTo("145.00");
        assertThat(discrepancy(paired, DiscrepancyType.MISSING_BANK_CREDIT).getActualValue()).isNull();
        assertThat(discrepancy(paired, DiscrepancyType.MISSING_BANK_CREDIT).getStatus())
                .isEqualTo(DiscrepancyStatus.OPEN);

        ReconciliationItemEntity fee = item(result, "TXN-B");
        assertThat(fee.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.FEE_DIVERGENCE, DiscrepancyType.MISSING_BANK_CREDIT);
        assertThat(fee.getResult()).isEqualTo(ReconciliationResult.DIVERGENT);

        ReconciliationItemEntity sale = item(result, "TXN-C");
        assertThat(sale.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.MISSING_SETTLEMENT);

        ReconciliationItemEntity orphanSettlement = item(result, "EXT-D");
        assertThat(orphanSettlement.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.ORPHAN_SETTLEMENT, DiscrepancyType.MISSING_BANK_CREDIT);
        assertThat(discrepancy(orphanSettlement, DiscrepancyType.MISSING_BANK_CREDIT).getExpectedValue())
                .isEqualTo("78.00");
        assertThat(discrepancy(orphanSettlement, DiscrepancyType.MISSING_BANK_CREDIT).getActualValue())
                .isNull();
    }

    @Test
    void shouldNotCreateAnotherItemWhenLineReferenceCollidesWithUnsettledSale() {
        List<ReconciliationItemEntity> sales = reconciliationEngine.reconcile(
                Map.of("LINE-1", transaction("LINE-1")),
                Map.of());
        BankStatementLineEntity line = bankLine("LINE-1", null, "50.00", SETTLEMENT_DATE);

        List<ReconciliationItemEntity> result = matcher.apply(sales, List.of(line), Set.of());

        assertThat(result).containsExactlyElementsOf(sales);
        assertThat(result.getFirst().getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.MISSING_SETTLEMENT);
        assertThat(result.getFirst().getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
    }

    @Test
    void shouldAttachBankDiscrepancyWhenLineReferenceCollidesWithSettlementItem() {
        ReconciliationItemEntity settlement = settlementItem("LINE-1", "100.00", SETTLEMENT_DATE);
        BankStatementLineEntity line = bankLine("LINE-1", null, "40.00", SETTLEMENT_DATE);

        List<ReconciliationItemEntity> result = matcher.apply(List.of(settlement), List.of(line), Set.of());

        assertThat(result).containsExactly(settlement);
        assertThat(settlement.getExternalSettlement()).isNotNull();
        assertThat(settlement.getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(settlement.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.MISSING_BANK_CREDIT, DiscrepancyType.ORPHAN_BANK_CREDIT);
        assertThat(discrepancy(settlement, DiscrepancyType.MISSING_BANK_CREDIT).getExpectedValue())
                .isEqualTo("100.00");
        assertThat(discrepancy(settlement, DiscrepancyType.MISSING_BANK_CREDIT).getActualValue()).isNull();
        assertThat(discrepancy(settlement, DiscrepancyType.ORPHAN_BANK_CREDIT).getExpectedValue()).isNull();
        assertThat(discrepancy(settlement, DiscrepancyType.ORPHAN_BANK_CREDIT).getActualValue())
                .isEqualTo("40.00");
        assertThat(discrepancy(settlement, DiscrepancyType.ORPHAN_BANK_CREDIT).getStatus())
                .isEqualTo(DiscrepancyStatus.OPEN);
    }

    private void assertAmbiguousSettlement(ReconciliationItemEntity settlement) {
        assertThat(settlement.getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(settlement.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.AMBIGUOUS_BANK_MATCH);
        ReconciliationDiscrepancyEntity discrepancy = discrepancy(
                settlement, DiscrepancyType.AMBIGUOUS_BANK_MATCH);
        assertThat(discrepancy.getExpectedValue()).isEqualTo("100.00");
        assertThat(discrepancy.getActualValue()).isNull();
        assertThat(discrepancy.getStatus()).isEqualTo(DiscrepancyStatus.OPEN);
    }

    private void assertAmbiguousBankLine(ReconciliationItemEntity lineItem, String amount) {
        assertThat(lineItem.getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(lineItem.getInternalTransaction()).isNull();
        assertThat(lineItem.getExternalSettlement()).isNull();
        assertThat(lineItem.getDiscrepancies())
                .extracting(ReconciliationDiscrepancyEntity::getType)
                .containsExactly(DiscrepancyType.AMBIGUOUS_BANK_MATCH);
        ReconciliationDiscrepancyEntity discrepancy = discrepancy(
                lineItem, DiscrepancyType.AMBIGUOUS_BANK_MATCH);
        assertThat(discrepancy.getExpectedValue()).isNull();
        assertThat(discrepancy.getActualValue()).isEqualTo(amount);
        assertThat(discrepancy.getStatus()).isEqualTo(DiscrepancyStatus.OPEN);
    }

    private ReconciliationItemEntity item(List<ReconciliationItemEntity> items, String reference) {
        return items.stream()
                .filter(candidate -> reference.equals(candidate.getExternalReference()))
                .findFirst()
                .orElseThrow();
    }

    private ReconciliationDiscrepancyEntity discrepancy(
            ReconciliationItemEntity item,
            DiscrepancyType type) {
        return item.getDiscrepancies().stream()
                .filter(candidate -> candidate.getType() == type)
                .findFirst()
                .orElseThrow();
    }

    private ReconciliationItemEntity settlementItem(String reference, String netAmount, LocalDate settlementDate) {
        ReconciliationItemEntity item = new ReconciliationItemEntity();
        item.setExternalReference(reference);
        item.setExternalSettlement(new ExternalSettlementEntity());
        item.setSettlementNetAmount(new BigDecimal(netAmount));
        item.setSettlementDate(settlementDate);
        item.setResult(ReconciliationResult.MATCHED);
        return item;
    }

    private BankStatementLineEntity bankLine(
            String lineReference,
            String externalReference,
            String amount,
            LocalDate movementDate) {
        BankStatementLineEntity line = new BankStatementLineEntity();
        line.setLineReference(lineReference);
        line.setExternalReference(externalReference);
        line.setAmount(new BigDecimal(amount));
        line.setMovementDate(movementDate);
        return line;
    }

    private static BankStatementMatcher matcherWithTolerance(String tolerance) {
        return new BankStatementMatcher(
                new ReconciliationProperties(new BigDecimal(tolerance), 5, 366, false, 1, 1));
    }

    private static ReconciliationEngine engineWithTolerance(String tolerance) {
        return new ReconciliationEngine(
                new ReconciliationProperties(new BigDecimal(tolerance), 5, 366, false, 1, 1));
    }

    private InternalTransactionEntity transaction(String externalReference) {
        return transaction(externalReference, "100.00", "97.00");
    }

    private InternalTransactionEntity transaction(
            String externalReference,
            String amount,
            String expectedNetAmount) {
        InternalTransactionEntity entity = new InternalTransactionEntity();
        entity.setMerchant(new MerchantEntity());
        entity.setExternalReference(externalReference);
        entity.setAmount(new BigDecimal(amount));
        entity.setExpectedNetAmount(new BigDecimal(expectedNetAmount));
        entity.setPaymentMethod(PaymentMethod.PIX);
        entity.setInstallments(1);
        entity.setStatus(TransactionStatus.APPROVED);
        entity.setTransactionDate(LocalDate.parse("2026-07-29"));
        return entity;
    }

    private ExternalSettlementEntity settlement(
            String externalReference,
            String amount,
            String netAmount) {
        ExternalSettlementEntity entity = new ExternalSettlementEntity();
        entity.setMerchant(new MerchantEntity());
        entity.setExternalReference(externalReference);
        entity.setAmount(new BigDecimal(amount));
        entity.setNetAmount(new BigDecimal(netAmount));
        entity.setPaymentMethod(PaymentMethod.PIX);
        entity.setInstallments(1);
        entity.setStatus(TransactionStatus.APPROVED);
        entity.setSettlementDate(SETTLEMENT_DATE);
        return entity;
    }
}
