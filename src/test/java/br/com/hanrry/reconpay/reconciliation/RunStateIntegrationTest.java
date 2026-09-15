package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.reconciliation.service.ReconciliationRunStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "reconpay.reconciliation.dispatch-delay-ms=3600000")
class RunStateIntegrationTest extends AbstractIntegrationTest {
    @Autowired ReconciliationRunStateService state;
    @Autowired IReconciliationRunRepository runs;
    @Autowired IMerchantRepository merchants;
    @Autowired PlatformTransactionManager transactions;

    @Test void claimCommitsIndependentlyAndRecoveryAndFailureAreDurable() {
        var merchant = new MerchantEntity();
        merchant.setName("Durable run");
        merchant.setDocument(UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        merchant.setActive(true);
        merchant = merchants.saveAndFlush(merchant);
        var run = new ReconciliationRunEntity();
        run.setMerchant(merchant);
        run.setFromDate(LocalDate.of(2026, 1, 1));
        run.setToDate(LocalDate.of(2026, 1, 2));
        run.setTotalItems(0); run.setMatchedCount(0); run.setDivergentCount(0);
        UUID id = runs.saveAndFlush(run).getId();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            assertThat(state.tryMarkRunning(id)).isTrue();
            tx.setRollbackOnly();
        });
        assertThat(runs.findById(id).orElseThrow().getStatus()).isEqualTo(ReconciliationRunStatus.RUNNING);
        assertThat(state.tryMarkRunning(id)).isFalse();
        state.returnToPending(id);
        assertThat(runs.findById(id).orElseThrow().getStartedAt()).isNull();
        assertThat(state.tryMarkRunning(id)).isTrue();
        state.recoverInterruptedRuns();
        assertThat(runs.findById(id).orElseThrow().getStatus()).isEqualTo(ReconciliationRunStatus.PENDING);
        assertThat(state.tryMarkRunning(id)).isTrue();
        state.markFailed(id);
        var failed = runs.findById(id).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(ReconciliationRunStatus.FAILED);
        assertThat(failed.getErrorMessage()).isEqualTo("Não foi possível concluir a conciliação.");
        assertThat(failed.getFinishedAt()).isNotNull();
    }
}
