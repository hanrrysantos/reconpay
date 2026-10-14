package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ReconciliationRunStateService {
    private final IReconciliationRunRepository repository;
    private final Clock clock;

    public boolean tryMarkRunning(UUID runId) {
        return repository.claimPending(runId, Instant.now(clock)) == 1;
    }

    public void returnToPending(UUID runId) {
        repository.requeueRunning(runId);
    }

    public void markFailed(UUID runId) {
        repository.failRunning(runId, Instant.now(clock), "Não foi possível concluir a conciliação.");
    }

    /*
     * The processor writes items in one long transaction; progress goes through
     * these separate commits so pollers see it before that transaction ends.
     */
    public void recordTotal(UUID runId, int total) {
        repository.recordTotal(runId, total);
    }

    public void recordProgress(UUID runId, int processed) {
        repository.recordProgress(runId, processed);
    }

    public void recoverInterruptedRuns() {
        repository.recoverRunning();
    }
}
