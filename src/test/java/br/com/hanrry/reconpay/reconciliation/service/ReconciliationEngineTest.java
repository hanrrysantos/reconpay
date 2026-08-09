package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
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

    private final ReconciliationEngine reconciliationEngine = new ReconciliationEngine();

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
    void shouldDetectIncorrectAmountAndStatusMismatch() {
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
                        DiscrepancyType.STATUS_MISMATCH);
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
