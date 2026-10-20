package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.exception.InvalidReconciliationWindowException;
import br.com.hanrry.reconpay.exception.PeriodConflictException;
import br.com.hanrry.reconpay.exception.PeriodNotFoundException;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodWindowPolicyTest {

    private static final LocalDate FROM = LocalDate.parse("2026-07-01");
    private static final LocalDate TO = LocalDate.parse("2026-07-15");
    private static final List<ReconciliationRunStatus> IN_FLIGHT = List.of(
            ReconciliationRunStatus.PENDING,
            ReconciliationRunStatus.RUNNING);

    @Mock
    private IReconciliationRunRepository runRepository;

    private PeriodWindowPolicy policy;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        policy = new PeriodWindowPolicy(
                runRepository,
                new ReconciliationProperties(new BigDecimal("0.00"), 5, 31, true, 2, 50, 300_000));
    }

    @Test
    void invertedDatesAreRejectedAsAnInvalidWindow() {
        assertThatThrownBy(() -> policy.ensureWindow(TO, FROM))
                .isInstanceOf(InvalidReconciliationWindowException.class)
                .hasMessage("fromDate deve ser anterior ou igual a toDate");
        verifyNoInteractions(runRepository);
    }

    @Test
    void windowLongerThanMaxDaysIsRejected() {
        assertThatThrownBy(() -> policy.ensureWindow(FROM, FROM.plusDays(31)))
                .isInstanceOf(InvalidReconciliationWindowException.class)
                .hasMessage("Janela de conciliação excede o máximo de 31 dias");
    }

    @Test
    void windowOfExactlyMaxDaysAndSingleDayAreAccepted() {
        assertThatCode(() -> policy.ensureWindow(FROM, FROM.plusDays(30))).doesNotThrowAnyException();
        assertThatCode(() -> policy.ensureWindow(FROM, FROM)).doesNotThrowAnyException();
    }

    @Test
    void pendingOrRunningRunIsAConflict() {
        when(runRepository.existsByMerchant_IdAndFromDateAndToDateAndStatusIn(merchantId, FROM, TO, IN_FLIGHT))
                .thenReturn(true);

        assertThatThrownBy(() -> policy.rejectInFlight(merchantId, FROM, TO))
                .isInstanceOf(PeriodConflictException.class)
                .hasMessage("Janela possui conciliação em andamento de 2026-07-01 a 2026-07-15");
    }

    @Test
    void windowWithoutRunInFlightPasses() {
        when(runRepository.existsByMerchant_IdAndFromDateAndToDateAndStatusIn(merchantId, FROM, TO, IN_FLIGHT))
                .thenReturn(false);

        assertThatCode(() -> policy.rejectInFlight(merchantId, FROM, TO)).doesNotThrowAnyException();
    }

    @Test
    void currentCompletedRunIsReturned() {
        ReconciliationRunEntity run = new ReconciliationRunEntity();
        when(runRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, FROM, TO, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.of(run));

        assertThat(policy.currentCompleted(merchantId, FROM, TO)).isSameAs(run);
    }

    @Test
    void missingCurrentCompletedRunIsNotFound() {
        when(runRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, FROM, TO, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> policy.currentCompleted(merchantId, FROM, TO))
                .isInstanceOf(PeriodNotFoundException.class)
                .hasMessage("Período não encontrado de 2026-07-01 a 2026-07-15");
    }
}
