package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.demo.config.DemoProperties;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.reconciliation.service.ReconciliationRunProcessor;
import br.com.hanrry.reconpay.reconciliation.service.ReconciliationRunStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/*
 * Finishes the demo's reconciliation inside the creation request, so the dashboard opens full.
 * The background dispatcher may claim the run first; then this only waits for its outcome.
 */
@Component
@RequiredArgsConstructor
public class DemoRunExecutor {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);

    private final ReconciliationRunStateService state;
    private final ReconciliationRunProcessor processor;
    private final IReconciliationRunRepository runRepository;
    private final DemoProperties properties;

    public void complete(UUID runId) {
        if (!state.tryMarkRunning(runId)) {
            awaitOutcome(runId);
            return;
        }
        try {
            processor.process(runId);
        } catch (RuntimeException ex) {
            state.markFailed(runId);
            throw ex;
        }
    }

    private void awaitOutcome(UUID runId) {
        long deadline = System.nanoTime() + properties.runWaitTimeout().toNanos();
        while (true) {
            ReconciliationRunStatus status = runRepository.findById(runId)
                    .map(ReconciliationRunEntity::getStatus)
                    .orElseThrow(() -> new IllegalStateException("Conciliação da demonstração não encontrada"));
            if (status == ReconciliationRunStatus.COMPLETED) {
                return;
            }
            if (status == ReconciliationRunStatus.FAILED) {
                throw new IllegalStateException("Conciliação da demonstração falhou");
            }
            if (System.nanoTime() - deadline >= 0) {
                throw new IllegalStateException("Conciliação da demonstração não terminou a tempo");
            }
            sleep();
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(POLL_INTERVAL);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Espera da conciliação da demonstração interrompida", ex);
        }
    }
}
