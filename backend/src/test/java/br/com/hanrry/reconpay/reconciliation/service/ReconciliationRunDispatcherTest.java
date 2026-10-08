package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import static org.mockito.Mockito.*;

class ReconciliationRunDispatcherTest {
    private final IReconciliationRunRepository repository = mock(IReconciliationRunRepository.class);
    private final ReconciliationRunStateService state = mock(ReconciliationRunStateService.class);
    private final ReconciliationRunProcessor processor = mock(ReconciliationRunProcessor.class);
    private final UUID id = UUID.randomUUID();

    @Test void claimsBeforeSubmittingAndDoesNotProcessUnclaimedRuns() {
        when(repository.findPendingIds(any())).thenReturn(List.of(id));
        when(state.tryMarkRunning(id)).thenReturn(true, false);
        var dispatcher = new ReconciliationRunDispatcher(repository, state, processor, Runnable::run);
        dispatcher.dispatch();
        dispatcher.dispatch();
        var order = inOrder(state, processor);
        order.verify(state).tryMarkRunning(id);
        order.verify(processor).process(id);
        verify(processor, times(1)).process(id);
    }

    @Test void rejectionRequeuesClaimedRun() {
        when(repository.findPendingIds(any())).thenReturn(List.of(id));
        when(state.tryMarkRunning(id)).thenReturn(true);
        new ReconciliationRunDispatcher(repository, state, processor,
                task -> { throw new RejectedExecutionException("saturated"); }).dispatch();
        verify(state).returnToPending(id);
        verifyNoInteractions(processor);
    }

    @Test void failureIsRecordedWithoutForwardingExceptionDetails() {
        when(repository.findPendingIds(any())).thenReturn(List.of(id));
        when(state.tryMarkRunning(id)).thenReturn(true);
        doThrow(new IllegalStateException("password=secret SQL")).when(processor).process(id);
        new ReconciliationRunDispatcher(repository, state, processor, Runnable::run).dispatch();
        verify(state).markFailed(id);
    }

    @Test void startupRecoversInterruptedWorkBeforePolling() {
        new ReconciliationRunDispatcher(repository, state, processor, Runnable::run).recoverInterruptedRuns();
        verify(state).recoverInterruptedRuns();
    }
}
