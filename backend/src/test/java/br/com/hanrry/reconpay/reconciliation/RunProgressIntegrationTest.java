package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.reconciliation.service.ReconciliationRunDispatcher;
import br.com.hanrry.reconpay.reconciliation.service.ReconciliationRunStateService;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import br.com.hanrry.reconpay.transaction.repository.IInternalTransactionRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

/*
 * The scheduler is parked so each test decides when its run is dispatched.
 * 501 sales produce two persistence batches (500 + 1), so progress has an
 * intermediate value to observe.
 */
@TestPropertySource(properties = "reconpay.reconciliation.dispatch-delay-ms=3600000")
class RunProgressIntegrationTest extends AbstractIntegrationTest {

    private static final int SALES = 501;

    @MockitoSpyBean ReconciliationRunStateService state;
    @Autowired ReconciliationRunDispatcher dispatcher;
    @Autowired IReconciliationRunRepository runs;
    @Autowired IMerchantRepository merchants;
    @Autowired IInternalTransactionRepository transactions;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;

    @Test
    void runPublishesCommittedProgressAndCompletesWithProcessedEqualToTotal() {
        MerchantEntity merchant = merchantWithSales("Progress merchant", LocalDate.of(2026, 3, 10));
        UUID runId = pendingRun(merchant, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        ReconciliationRunEntity pending = runs.findById(runId).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(ReconciliationRunStatus.PENDING);
        assertThat(pending.getProcessedItems()).isNull();

        /*
         * The spy sits inside the transactional proxy, so each answer runs
         * before its own write commits. Reading first shows what the previous
         * write committed while the processor's transaction is still open.
         */
        List<String> observed = new CopyOnWriteArrayList<>();
        doAnswer(invocation -> {
            observed.add(committedProgress(invocation.getArgument(0)));
            return invocation.callRealMethod();
        }).when(state).recordTotal(any(), anyInt());
        doAnswer(invocation -> {
            observed.add(committedProgress(invocation.getArgument(0)));
            return invocation.callRealMethod();
        }).when(state).recordProgress(any(), anyInt());

        dispatcher.dispatch();
        awaitStatus(runId, ReconciliationRunStatus.COMPLETED);

        assertThat(observed).containsExactly(
                "RUNNING/null/0",
                "RUNNING/0/" + SALES,
                "RUNNING/500/" + SALES);

        ReconciliationRunEntity completed = runs.findById(runId).orElseThrow();
        assertThat(completed.getTotalItems()).isEqualTo(SALES);
        assertThat(completed.getProcessedItems()).isEqualTo(completed.getTotalItems());
        assertThat(completed.isBankTracked()).isTrue();
    }

    @Test
    void runWithoutItemsCompletesWithProcessedEqualToTotal() {
        MerchantEntity merchant = merchant("Empty merchant");
        UUID runId = pendingRun(merchant, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        dispatcher.dispatch();
        awaitStatus(runId, ReconciliationRunStatus.COMPLETED);

        ReconciliationRunEntity completed = runs.findById(runId).orElseThrow();
        assertThat(completed.getTotalItems()).isZero();
        assertThat(completed.getProcessedItems()).isZero();
        assertThat(completed.isBankTracked()).isTrue();
    }

    @Test
    void failureMidRunLeavesTheRunFailedWithoutPersistedItems() {
        MerchantEntity merchant = merchantWithSales("Failing merchant", LocalDate.of(2026, 4, 10));
        UUID runId = pendingRun(merchant, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));

        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("simulated worker crash after the first batch");
        }).when(state).recordProgress(any(), anyInt());

        dispatcher.dispatch();
        awaitStatus(runId, ReconciliationRunStatus.FAILED);

        ReconciliationRunEntity failed = runs.findById(runId).orElseThrow();
        assertThat(failed.isBankTracked()).isFalse();
        assertThat(jdbc.queryForObject(
                "select count(*) from reconciliation_items where reconciliation_run_id = ?",
                Integer.class, runId)).isZero();
    }

    private String committedProgress(UUID runId) {
        TransactionTemplate reader = new TransactionTemplate(transactionManager);
        reader.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        ReconciliationRunEntity run = reader.execute(status -> runs.findById(runId).orElseThrow());
        return run.getStatus() + "/" + run.getProcessedItems() + "/" + run.getTotalItems();
    }

    private void awaitStatus(UUID runId, ReconciliationRunStatus expected) {
        Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(runs.findById(runId).orElseThrow().getStatus()).isEqualTo(expected));
    }

    private MerchantEntity merchant(String name) {
        MerchantEntity merchant = new MerchantEntity();
        merchant.setName(name);
        merchant.setDocument(UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        merchant.setActive(true);
        return merchants.saveAndFlush(merchant);
    }

    private MerchantEntity merchantWithSales(String name, LocalDate saleDate) {
        MerchantEntity saved = merchant(name);

        List<InternalTransactionEntity> sales = new ArrayList<>();
        IntStream.range(0, SALES).forEach(index -> {
            InternalTransactionEntity sale = new InternalTransactionEntity();
            sale.setMerchant(saved);
            sale.setExternalReference("TXN-" + index + "-" + UUID.randomUUID());
            sale.setAmount(new BigDecimal("100.00"));
            sale.setExpectedNetAmount(new BigDecimal("97.00"));
            sale.setPaymentMethod(PaymentMethod.PIX);
            sale.setInstallments(1);
            sale.setStatus(TransactionStatus.APPROVED);
            sale.setTransactionDate(saleDate);
            sales.add(sale);
        });
        transactions.saveAll(sales);
        return saved;
    }

    private UUID pendingRun(MerchantEntity merchant, LocalDate fromDate, LocalDate toDate) {
        ReconciliationRunEntity run = new ReconciliationRunEntity();
        run.setMerchant(merchant);
        run.setFromDate(fromDate);
        run.setToDate(toDate);
        run.setTotalItems(0);
        run.setMatchedCount(0);
        run.setDivergentCount(0);
        return runs.saveAndFlush(run).getId();
    }
}
