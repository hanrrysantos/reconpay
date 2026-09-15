package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
@Component
public class ReconciliationRunDispatcher {
    private final IReconciliationRunRepository repository;
    private final ReconciliationRunStateService state;
    private final ReconciliationRunProcessor processor;
    private final Executor executor;
    private volatile boolean ready;

    public ReconciliationRunDispatcher(IReconciliationRunRepository repository,
            ReconciliationRunStateService state, ReconciliationRunProcessor processor,
            @Qualifier("reconciliationExecutor") Executor executor) {
        this.repository = repository;
        this.state = state;
        this.processor = processor;
        this.executor = executor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void recoverInterruptedRuns() {
        state.recoverInterruptedRuns();
        ready = true;
    }

    @Scheduled(fixedDelayString = "${reconpay.reconciliation.dispatch-delay-ms:1000}")
    public void poll() {
        if (ready) dispatch();
    }

    public synchronized void dispatch() {
        for (UUID id : repository.findPendingIds(PageRequest.of(0, 100))) {
            if (!state.tryMarkRunning(id)) continue;
            try {
                executor.execute(() -> execute(id));
            } catch (RejectedExecutionException ex) {
                state.returnToPending(id);
                log.warn("Executor ocupado; conciliação permanece pendente | runId={}", id);
                break;
            }
        }
    }

    private void execute(UUID id) {
        String previous = MDC.get("runId");
        MDC.put("runId", id.toString());
        try {
            processor.process(id);
        } catch (Exception ex) {
            log.error("Falha ao executar conciliação | runId={}", id, ex);
            state.markFailed(id);
        } finally {
            if (previous == null) MDC.remove("runId"); else MDC.put("runId", previous);
        }
    }
}
