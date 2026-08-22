package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
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

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationEngineTest {

    private final ReconciliationEngine reconciliationEngine = engineWithTolerance("0.00");

    private static ReconciliationEngine engineWithTolerance(String tolerance) {
        return new ReconciliationEngine(
                new ReconciliationProperties(new BigDecimal(tolerance), 5, 366));
    }

    @Test
    void shouldMatchWhenAllFieldsAreEqual() {
        InternalTransactionEntity transaction = buildTransaction(
                "TXN-001",
                new BigDecimal("150.00"),
                new BigDecimal("145.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        ExternalSettlementEntity settlement = buildSettlement(
                "TXN-001",
                new BigDecimal("150.00"),
                new BigDecimal("145.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        List<ReconciliationItemEntity> items = reconciliationEngine.reconcile(
                Map.of("TXN-001", transaction),
                Map.of("TXN-001", settlement));

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getResult()).isEqualTo(ReconciliationResult.MATCHED);
        assertThat(items.getFirst().getDiscrepancies()).isEmpty();
    }

    @Test
    void shouldDetectMissingSettlement() {
        InternalTransactionEntity transaction = buildTransaction(
                "TXN-002",
                new BigDecimal("100.00"),
                new BigDecimal("97.00"),
                PaymentMethod.PIX,
                1,
                TransactionStatus.APPROVED);

        List<ReconciliationItemEntity> items = reconciliationEngine.reconcile(
                Map.of("TXN-002", transaction),
                Map.of());

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(items.getFirst().getDiscrepancies())
                .extracting(discrepancy -> discrepancy.getType())
                .containsExactly(DiscrepancyType.MISSING_SETTLEMENT);
    }

    @Test
    void shouldDetectOrphanSettlement() {
        ExternalSettlementEntity settlement = buildSettlement(
                "EXT-001",
                new BigDecimal("80.00"),
                new BigDecimal("78.00"),
                PaymentMethod.PIX,
                1,
                TransactionStatus.APPROVED);

        List<ReconciliationItemEntity> items = reconciliationEngine.reconcile(
                Map.of(),
                Map.of("EXT-001", settlement));

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(items.getFirst().getDiscrepancies())
                .extracting(discrepancy -> discrepancy.getType())
                .containsExactly(DiscrepancyType.ORPHAN_SETTLEMENT);
    }

    @Test
    void shouldDetectFeeDivergenceWhenGrossAmountMatches() {
        InternalTransactionEntity transaction = buildTransaction(
                "TXN-003",
                new BigDecimal("150.00"),
                new BigDecimal("145.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        ExternalSettlementEntity settlement = buildSettlement(
                "TXN-003",
                new BigDecimal("150.00"),
                new BigDecimal("140.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        List<ReconciliationItemEntity> items = reconciliationEngine.reconcile(
                Map.of("TXN-003", transaction),
                Map.of("TXN-003", settlement));

        assertThat(items.getFirst().getDiscrepancies())
                .extracting(discrepancy -> discrepancy.getType())
                .contains(DiscrepancyType.FEE_DIVERGENCE);
    }

    @Test
    void shouldDetectFeeDivergenceEvenWhenGrossAmountAlsoDiverges() {
        InternalTransactionEntity transaction = buildTransaction(
                "TXN-004",
                new BigDecimal("150.00"),
                new BigDecimal("145.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        ExternalSettlementEntity settlement = buildSettlement(
                "TXN-004",
                new BigDecimal("160.00"),
                new BigDecimal("155.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.CHARGEBACK);

        List<ReconciliationItemEntity> items = reconciliationEngine.reconcile(
                Map.of("TXN-004", transaction),
                Map.of("TXN-004", settlement));

        assertThat(items.getFirst().getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(items.getFirst().getDiscrepancies())
                .extracting(discrepancy -> discrepancy.getType())
                .containsExactlyInAnyOrder(
                        DiscrepancyType.INCORRECT_AMOUNT,
                        DiscrepancyType.FEE_DIVERGENCE,
                        DiscrepancyType.STATUS_MISMATCH);
    }

    @Test
    void shouldDetectPaymentMethodMismatch() {
        InternalTransactionEntity transaction = buildTransaction(
                "TXN-005",
                new BigDecimal("200.00"),
                new BigDecimal("194.00"),
                PaymentMethod.CREDIT_CARD,
                1,
                TransactionStatus.APPROVED);

        ExternalSettlementEntity settlement = buildSettlement(
                "TXN-005",
                new BigDecimal("200.00"),
                new BigDecimal("194.00"),
                PaymentMethod.DEBIT_CARD,
                1,
                TransactionStatus.APPROVED);

        List<ReconciliationItemEntity> items = reconciliationEngine.reconcile(
                Map.of("TXN-005", transaction),
                Map.of("TXN-005", settlement));

        assertThat(items.getFirst().getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(items.getFirst().getDiscrepancies())
                .extracting(discrepancy -> discrepancy.getType())
                .containsExactly(DiscrepancyType.PAYMENT_METHOD_MISMATCH);
    }

    @Test
    void shouldDetectInstallmentsMismatch() {
        InternalTransactionEntity transaction = buildTransaction(
                "TXN-006",
                new BigDecimal("300.00"),
                new BigDecimal("291.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        ExternalSettlementEntity settlement = buildSettlement(
                "TXN-006",
                new BigDecimal("300.00"),
                new BigDecimal("291.00"),
                PaymentMethod.CREDIT_CARD,
                6,
                TransactionStatus.APPROVED);

        List<ReconciliationItemEntity> items = reconciliationEngine.reconcile(
                Map.of("TXN-006", transaction),
                Map.of("TXN-006", settlement));

        assertThat(items.getFirst().getResult()).isEqualTo(ReconciliationResult.DIVERGENT);
        assertThat(items.getFirst().getDiscrepancies())
                .extracting(discrepancy -> discrepancy.getType())
                .containsExactly(DiscrepancyType.INSTALLMENTS_MISMATCH);
    }

    @Test
    void shouldMatchAmountsThatDifferOnlyInScale() {
        InternalTransactionEntity transaction = buildTransaction(
                "TXN-007",
                new BigDecimal("150.0"),
                new BigDecimal("145.000"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        ExternalSettlementEntity settlement = buildSettlement(
                "TXN-007",
                new BigDecimal("150.00"),
                new BigDecimal("145.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        List<ReconciliationItemEntity> items = reconciliationEngine.reconcile(
                Map.of("TXN-007", transaction),
                Map.of("TXN-007", settlement));

        assertThat(items.getFirst().getResult()).isEqualTo(ReconciliationResult.MATCHED);
        assertThat(items.getFirst().getDiscrepancies()).isEmpty();
    }

    @Test
    void shouldDetectEveryDiscrepancyTypeAtOnce() {
        InternalTransactionEntity transaction = buildTransaction(
                "TXN-008",
                new BigDecimal("150.00"),
                new BigDecimal("145.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        ExternalSettlementEntity settlement = buildSettlement(
                "TXN-008",
                new BigDecimal("160.00"),
                new BigDecimal("150.00"),
                PaymentMethod.PIX,
                1,
                TransactionStatus.CHARGEBACK);

        List<ReconciliationItemEntity> items = reconciliationEngine.reconcile(
                Map.of("TXN-008", transaction),
                Map.of("TXN-008", settlement));

        assertThat(items.getFirst().getDiscrepancies())
                .extracting(discrepancy -> discrepancy.getType())
                .containsExactlyInAnyOrder(
                        DiscrepancyType.INCORRECT_AMOUNT,
                        DiscrepancyType.FEE_DIVERGENCE,
                        DiscrepancyType.STATUS_MISMATCH,
                        DiscrepancyType.PAYMENT_METHOD_MISMATCH,
                        DiscrepancyType.INSTALLMENTS_MISMATCH);
    }

    @Test
    void shouldReturnNoItemsWhenBothSidesAreEmpty() {
        assertThat(reconciliationEngine.reconcile(Map.of(), Map.of())).isEmpty();
    }

    @Test
    void shouldSnapshotBothSidesOnTheItem() {
        InternalTransactionEntity transaction = buildTransaction(
                "TXN-009",
                new BigDecimal("150.00"),
                new BigDecimal("145.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        ExternalSettlementEntity settlement = buildSettlement(
                "TXN-009",
                new BigDecimal("150.00"),
                new BigDecimal("145.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        ReconciliationItemEntity item = reconciliationEngine.reconcile(
                Map.of("TXN-009", transaction),
                Map.of("TXN-009", settlement)).getFirst();

        assertThat(item.getTransactionAmount()).isEqualByComparingTo("150.00");
        assertThat(item.getExpectedNetAmount()).isEqualByComparingTo("145.00");
        assertThat(item.getTransactionPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(item.getTransactionInstallments()).isEqualTo(3);
        assertThat(item.getTransactionStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(item.getTransactionDate()).isEqualTo(LocalDate.parse("2026-07-29"));
        assertThat(item.getSettlementAmount()).isEqualByComparingTo("150.00");
        assertThat(item.getSettlementNetAmount()).isEqualByComparingTo("145.00");
        assertThat(item.getSettlementDate()).isEqualTo(LocalDate.parse("2026-07-30"));
    }

    @Test
    void shouldLeaveSnapshotOfTheMissingSideNull() {
        InternalTransactionEntity transaction = buildTransaction(
                "TXN-010",
                new BigDecimal("100.00"),
                new BigDecimal("97.00"),
                PaymentMethod.PIX,
                1,
                TransactionStatus.APPROVED);

        ReconciliationItemEntity item = reconciliationEngine.reconcile(
                Map.of("TXN-010", transaction),
                Map.of()).getFirst();

        assertThat(item.getTransactionAmount()).isEqualByComparingTo("100.00");
        assertThat(item.getSettlementAmount()).isNull();
        assertThat(item.getSettlementNetAmount()).isNull();
        assertThat(item.getSettlementStatus()).isNull();
        assertThat(item.getSettlementDate()).isNull();
    }

    @Test
    void shouldNotFlagAmountDifferenceWithinConfiguredTolerance() {
        ReconciliationEngine tolerantEngine = engineWithTolerance("0.05");

        InternalTransactionEntity transaction = buildTransaction(
                "TXN-011",
                new BigDecimal("150.00"),
                new BigDecimal("145.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        ExternalSettlementEntity settlement = buildSettlement(
                "TXN-011",
                new BigDecimal("150.03"),
                new BigDecimal("144.98"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        ReconciliationItemEntity item = tolerantEngine.reconcile(
                Map.of("TXN-011", transaction),
                Map.of("TXN-011", settlement)).getFirst();

        assertThat(item.getResult()).isEqualTo(ReconciliationResult.MATCHED);
    }

    @Test
    void shouldFlagAmountDifferenceBeyondConfiguredTolerance() {
        ReconciliationEngine tolerantEngine = engineWithTolerance("0.05");

        InternalTransactionEntity transaction = buildTransaction(
                "TXN-012",
                new BigDecimal("150.00"),
                new BigDecimal("145.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        ExternalSettlementEntity settlement = buildSettlement(
                "TXN-012",
                new BigDecimal("150.06"),
                new BigDecimal("145.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED);

        ReconciliationItemEntity item = tolerantEngine.reconcile(
                Map.of("TXN-012", transaction),
                Map.of("TXN-012", settlement)).getFirst();

        assertThat(item.getDiscrepancies())
                .extracting(discrepancy -> discrepancy.getType())
                .containsExactly(DiscrepancyType.INCORRECT_AMOUNT);
    }

    private InternalTransactionEntity buildTransaction(
            String externalReference,
            BigDecimal amount,
            BigDecimal expectedNetAmount,
            PaymentMethod paymentMethod,
            Integer installments,
            TransactionStatus status) {
        InternalTransactionEntity entity = new InternalTransactionEntity();
        entity.setMerchant(new MerchantEntity());
        entity.setExternalReference(externalReference);
        entity.setAmount(amount);
        entity.setExpectedNetAmount(expectedNetAmount);
        entity.setPaymentMethod(paymentMethod);
        entity.setInstallments(installments);
        entity.setStatus(status);
        entity.setTransactionDate(LocalDate.parse("2026-07-29"));
        return entity;
    }

    private ExternalSettlementEntity buildSettlement(
            String externalReference,
            BigDecimal amount,
            BigDecimal netAmount,
            PaymentMethod paymentMethod,
            Integer installments,
            TransactionStatus status) {
        ExternalSettlementEntity entity = new ExternalSettlementEntity();
        entity.setMerchant(new MerchantEntity());
        entity.setExternalReference(externalReference);
        entity.setAmount(amount);
        entity.setNetAmount(netAmount);
        entity.setPaymentMethod(paymentMethod);
        entity.setInstallments(installments);
        entity.setStatus(status);
        entity.setSettlementDate(LocalDate.parse("2026-07-30"));
        return entity;
    }
}
