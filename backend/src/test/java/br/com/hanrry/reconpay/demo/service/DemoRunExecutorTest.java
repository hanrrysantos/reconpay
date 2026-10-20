package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.demo.config.DemoProperties;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.reconciliation.service.ReconciliationRunProcessor;
import br.com.hanrry.reconpay.reconciliation.service.ReconciliationRunStateService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DemoRunExecutorTest {

    private final ReconciliationRunStateService state = mock(ReconciliationRunStateService.class);
    private final ReconciliationRunProcessor processor = mock(ReconciliationRunProcessor.class);
    private final IReconciliationRunRepository runRepository = mock(IReconciliationRunRepository.class);
    private final UUID runId = UUID.randomUUID();

    @Test
    void claimedRunIsProcessedOnTheCallingThread() {
        when(state.tryMarkRunning(runId)).thenReturn(true);

        executor(Duration.ofSeconds(30)).complete(runId);

        var order = inOrder(state, processor);
        order.verify(state).tryMarkRunning(runId);
        order.verify(processor).process(runId);
        verify(state, never()).markFailed(runId);
        verifyNoInteractions(runRepository);
    }

    @Test
    void processingFailureMarksTheRunFailedAndPropagates() {
        when(state.tryMarkRunning(runId)).thenReturn(true);
        IllegalStateException failure = new IllegalStateException("engine broke");
        doThrow(failure).when(processor).process(runId);

        assertThatThrownBy(() -> executor(Duration.ofSeconds(30)).complete(runId)).isSameAs(failure);

        verify(state).markFailed(runId);
    }

    @Test
    void runClaimedByTheDispatcherIsAwaitedUntilCompleted() {
        when(state.tryMarkRunning(runId)).thenReturn(false);
        when(runRepository.findById(runId)).thenReturn(
                run(ReconciliationRunStatus.RUNNING), run(ReconciliationRunStatus.COMPLETED));

        assertThatCode(() -> executor(Duration.ofSeconds(30)).complete(runId)).doesNotThrowAnyException();

        verify(runRepository, times(2)).findById(runId);
        verifyNoInteractions(processor);
    }

    @Test
    void runClaimedByTheDispatcherThatFailsIsReportedAsFailure() {
        when(state.tryMarkRunning(runId)).thenReturn(false);
        when(runRepository.findById(runId)).thenReturn(
                run(ReconciliationRunStatus.RUNNING), run(ReconciliationRunStatus.FAILED));

        assertThatThrownBy(() -> executor(Duration.ofSeconds(30)).complete(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Conciliação da demonstração falhou");

        verify(runRepository, times(2)).findById(runId);
        verifyNoInteractions(processor);
    }

    @Test
    void runStillRunningAfterTheWaitTimeoutFails() {
        when(state.tryMarkRunning(runId)).thenReturn(false);
        when(runRepository.findById(runId)).thenReturn(run(ReconciliationRunStatus.RUNNING));
        long started = System.nanoTime();

        assertThatThrownBy(() -> executor(Duration.ofMillis(300)).complete(runId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Conciliação da demonstração não terminou a tempo");

        assertThat(Duration.ofNanos(System.nanoTime() - started)).isGreaterThanOrEqualTo(Duration.ofMillis(300));
        verifyNoInteractions(processor);
    }

    private DemoRunExecutor executor(Duration runWaitTimeout) {
        DemoProperties properties = new DemoProperties(Duration.ofHours(24), 100, 2, 2, 10, 900000, runWaitTimeout);
        return new DemoRunExecutor(state, processor, runRepository, properties);
    }

    private static Optional<ReconciliationRunEntity> run(ReconciliationRunStatus status) {
        ReconciliationRunEntity run = new ReconciliationRunEntity();
        run.setStatus(status);
        return Optional.of(run);
    }
}
