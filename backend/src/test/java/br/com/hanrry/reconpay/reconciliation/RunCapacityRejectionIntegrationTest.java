package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationItemRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.reconciliation.service.ReconciliationRunDispatcher;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import br.com.hanrry.reconpay.transaction.repository.IInternalTransactionRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/*
 * CAP-02: a run already queued is counted again when the worker claims it. The
 * scheduler is parked and the limit lowered to 3, and each run is stored
 * straight as PENDING, as it would be after a restart requeued it.
 */
@TestPropertySource(properties = {
        "reconpay.reconciliation.dispatch-delay-ms=3600000",
        "reconpay.reconciliation.max-window-records=3"})
class RunCapacityRejectionIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate FROM = LocalDate.of(2026, 4, 1);
    private static final LocalDate TO = LocalDate.of(2026, 4, 30);
    private static final String REJECTION =
            "A janela tem 4 registros para conciliar; o limite é 3. Divida o período em janelas menores.";

    @MockitoSpyBean IInternalTransactionRepository transactions;
    @MockitoSpyBean AuditLogger auditLogger;
    @Autowired ReconciliationRunDispatcher dispatcher;
    @Autowired IReconciliationRunRepository runs;
    @Autowired IReconciliationItemRepository items;
    @Autowired IMerchantRepository merchants;

    @Test
    @SuppressWarnings("unchecked")
    void aQueuedRunOverTheLimitFailsWithTheMessageWithoutLoadingTheWindow() {
        UUID runId = pendingRun(merchantWithSales("Over the limit", 4));
        clearInvocations(transactions);

        dispatcher.dispatch();
        awaitStatus(runId, ReconciliationRunStatus.FAILED);

        ReconciliationRunEntity failed = runs.findById(runId).orElseThrow();
        assertThat(failed.getErrorMessage()).isEqualTo(REJECTION);
        assertThat(failed.getFinishedAt()).isNotNull();
        assertThat(items.findIdsByRunId(runId, PageRequest.of(0, 1))).isEmpty();
        verify(transactions, never()).findAll(any(Specification.class));
        verify(auditLogger).record(
                eq("RECONCILIATION_REJECTED"), eq("reconciliationRun"), eq(runId), contains("limit=3"));
        verify(auditLogger).record(
                eq("RECONCILIATION_REJECTED"), eq("reconciliationRun"), eq(runId), contains("4 registros"));
    }

    @Test
    void aQueuedRunAtTheLimitCompletesAsBefore() {
        UUID runId = pendingRun(merchantWithSales("At the limit", 3));

        dispatcher.dispatch();
        awaitStatus(runId, ReconciliationRunStatus.COMPLETED);

        ReconciliationRunEntity completed = runs.findById(runId).orElseThrow();
        assertThat(completed.getErrorMessage()).isNull();
        assertThat(completed.getTotalItems()).isEqualTo(3);
    }

    private MerchantEntity merchantWithSales(String name, int count) {
        MerchantEntity merchant = new MerchantEntity();
        merchant.setName(name);
        merchant.setDocument(UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        merchant.setActive(true);
        MerchantEntity saved = merchants.saveAndFlush(merchant);

        List<InternalTransactionEntity> sales = IntStream.range(0, count).mapToObj(index -> {
            InternalTransactionEntity sale = new InternalTransactionEntity();
            sale.setMerchant(saved);
            sale.setExternalReference("CAP-" + index + "-" + UUID.randomUUID());
            sale.setAmount(new BigDecimal("100.00"));
            sale.setExpectedNetAmount(new BigDecimal("99.00"));
            sale.setPaymentMethod(PaymentMethod.PIX);
            sale.setInstallments(1);
            sale.setStatus(TransactionStatus.APPROVED);
            sale.setTransactionDate(FROM.plusDays(index));
            return sale;
        }).toList();
        transactions.saveAll(sales);
        return saved;
    }

    private UUID pendingRun(MerchantEntity merchant) {
        ReconciliationRunEntity run = new ReconciliationRunEntity();
        run.setMerchant(merchant);
        run.setFromDate(FROM);
        run.setToDate(TO);
        run.setTotalItems(0);
        run.setMatchedCount(0);
        run.setDivergentCount(0);
        return runs.saveAndFlush(run).getId();
    }

    private void awaitStatus(UUID runId, ReconciliationRunStatus expected) {
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(runs.findById(runId).orElseThrow().getStatus()).isEqualTo(expected));
    }
}
