package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.reconciliation.event.ReconciliationRunRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationRunListener {

    private final ReconciliationRunProcessor processor;

    /*
     * AFTER_COMMIT so the worker never looks for a run that the request rolled
     * back. Any failure is recorded on the run itself, because nobody is left on
     * the HTTP call to receive it.
     */
    @Async("reconciliationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRunRequested(ReconciliationRunRequestedEvent event) {
        try {
            processor.process(event.runId());
        } catch (Exception ex) {
            log.error("Falha ao executar conciliação | runId={}", event.runId(), ex);
            processor.markFailed(event.runId(), ex.getMessage());
        }
    }
}
