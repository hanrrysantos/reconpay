package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.exception.InvalidReconciliationWindowException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.PeriodConflictException;
import br.com.hanrry.reconpay.exception.PeriodNotFoundException;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.reconciliation.dto.PeriodResponseDTO;
import br.com.hanrry.reconpay.reconciliation.entity.PeriodLockEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IPeriodLockRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationDiscrepancyRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodServiceTest {

    private static final LocalDate FROM = LocalDate.parse("2026-07-01");
    private static final LocalDate TO = LocalDate.parse("2026-07-15");
    private static final Instant FINISHED_AT = Instant.parse("2026-07-16T10:00:00.900Z");
    private static final Instant LOCKED_AT = Instant.parse("2026-07-16T10:00:30.400Z");
    private static final Instant ORIGINAL_LOCKED_AT = Instant.parse("2026-07-16T10:00:10.000Z");
    private static final long CLOSE_SECONDS = 29L;
    private static final List<ReconciliationRunStatus> IN_FLIGHT = List.of(
            ReconciliationRunStatus.PENDING,
            ReconciliationRunStatus.RUNNING);

    @Mock
    private PeriodGuard periodGuard;

    @Mock
    private IReconciliationRunRepository reconciliationRunRepository;

    @Mock
    private IPeriodLockRepository periodLockRepository;

    @Mock
    private IReconciliationDiscrepancyRepository discrepancyRepository;

    @Mock
    private IMerchantRepository merchantRepository;

    @Mock
    private AuditLogger auditLogger;

    private final MutableClock clock = new MutableClock(LOCKED_AT);

    private PeriodService periodService;
    private UUID merchantId;
    private UUID runId;
    private MerchantEntity merchant;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        runId = UUID.randomUUID();
        merchant = new MerchantEntity();
        merchant.setId(merchantId);
        merchant.setName("Period merchant");
        merchant.setActive(true);
        clock.set(LOCKED_AT);
        periodService = new PeriodService(
                periodGuard,
                new OpenAmountCalculator(),
                reconciliationRunRepository,
                periodLockRepository,
                discrepancyRepository,
                merchantRepository,
                new ReconciliationProperties(new BigDecimal("0.00"), 5, 366, true, 2, 50),
                auditLogger,
                clock);
    }

    @Test
    void shouldReturnFrozenMatchRateOpenAmountAndCloseDuration() {
        ReconciliationRunEntity run = completedRun(2, 1, 1);
        stubActiveMerchant();
        stubNoInFlight();
        stubCurrentRun(run);
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.of(lock(run, LOCKED_AT)));
        when(discrepancyRepository.findByRunIdWithItem(runId))
                .thenReturn(List.of(feeGap(DiscrepancyStatus.OPEN)));
        clearInvocations(periodGuard, merchantRepository, reconciliationRunRepository, periodLockRepository);

        PeriodResponseDTO response = periodService.get(merchantId, FROM, TO);

        assertThat(response.runId()).isEqualTo(runId);
        assertThat(response.fromDate()).isEqualTo(FROM);
        assertThat(response.toDate()).isEqualTo(TO);
        assertThat(response.totalItems()).isEqualTo(2);
        assertThat(response.matchedCount()).isEqualTo(1);
        assertThat(response.divergentCount()).isEqualTo(1);
        assertThat(response.matchRate()).isEqualTo(new BigDecimal("0.5000"));
        assertThat(response.openAmount()).isEqualTo(new BigDecimal("2.80"));
        assertThat(response.locked()).isTrue();
        assertThat(response.lockedAt()).isEqualTo(LOCKED_AT);
        assertThat(response.closeDurationSeconds()).isEqualTo(CLOSE_SECONDS);
        verifyNoInteractions(periodGuard);
    }

    @Test
    void shouldRoundMatchRateHalfUpToFourDecimalPlaces() {
        ReconciliationRunEntity run = completedRun(6, 1, 0);
        stubReadableWindow(run, null);

        PeriodResponseDTO response = periodService.get(merchantId, FROM, TO);

        assertThat(response.matchRate()).isEqualTo(new BigDecimal("0.1667"));
        assertThat(response.totalItems()).isEqualTo(6);
        assertThat(response.matchedCount()).isEqualTo(1);
    }

    @Test
    void shouldLeaveMatchRateNullWhenTotalItemsIsZero() {
        ReconciliationRunEntity run = completedRun(0, 0, 0);
        stubReadableWindow(run, null);

        PeriodResponseDTO response = periodService.get(merchantId, FROM, TO);

        assertThat(response.totalItems()).isZero();
        assertThat(response.matchRate()).isNull();
        assertThat(response.openAmount()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void shouldLeaveLockFieldsEmptyWhenTheWindowIsOpen() {
        ReconciliationRunEntity run = completedRun(2, 1, 1);
        stubReadableWindow(run, null);

        PeriodResponseDTO response = periodService.get(merchantId, FROM, TO);

        assertThat(response.locked()).isFalse();
        assertThat(response.lockedAt()).isNull();
        assertThat(response.closeDurationSeconds()).isNull();
        assertThat(response.runId()).isEqualTo(runId);
    }

    @Test
    void shouldKeepMatchRateWhenTheDiscrepancyIsNoLongerOpen() {
        ReconciliationRunEntity run = completedRun(2, 1, 1);
        stubActiveMerchant();
        stubNoInFlight();
        stubCurrentRun(run);
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.empty());
        when(discrepancyRepository.findByRunIdWithItem(runId))
                .thenReturn(List.of(feeGap(DiscrepancyStatus.ACCEPTED)));

        PeriodResponseDTO response = periodService.get(merchantId, FROM, TO);

        assertThat(response.matchRate()).isEqualTo(new BigDecimal("0.5000"));
        assertThat(response.matchedCount()).isEqualTo(1);
        assertThat(response.totalItems()).isEqualTo(2);
        assertThat(response.openAmount()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void shouldReturnTheCurrentCompletedRunWhenAFailedRunAlsoExists() {
        ReconciliationRunEntity completed = completedRun(2, 2, 0);
        ReconciliationRunEntity failed = completedRun(9, 0, 9);
        failed.setId(UUID.randomUUID());
        failed.setStatus(ReconciliationRunStatus.FAILED);
        failed.setSupersededAt(null);
        stubReadableWindow(completed, null);

        PeriodResponseDTO response = periodService.get(merchantId, FROM, TO);

        assertThat(response.runId()).isEqualTo(completed.getId());
        assertThat(response.runId()).isNotEqualTo(failed.getId());
        assertThat(response.totalItems()).isEqualTo(2);
        assertThat(response.matchedCount()).isEqualTo(2);
        verify(reconciliationRunRepository).findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, FROM, TO, ReconciliationRunStatus.COMPLETED);
        verify(reconciliationRunRepository, never())
                .findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                        merchantId, FROM, TO, ReconciliationRunStatus.FAILED);
    }

    @Test
    void shouldRejectGetWhenNoCurrentCompletedRunExists() {
        stubActiveMerchant();
        stubNoInFlight();
        when(reconciliationRunRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, FROM, TO, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> periodService.get(merchantId, FROM, TO))
                .isInstanceOf(PeriodNotFoundException.class);
        verifyNoInteractions(periodLockRepository);
    }

    @Test
    void shouldRejectGetWhenAPendingRunExists() {
        stubActiveMerchant();
        stubInFlight();

        assertThatThrownBy(() -> periodService.get(merchantId, FROM, TO))
                .isInstanceOf(PeriodConflictException.class);
        verify(reconciliationRunRepository, never())
                .findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                        any(), any(), any(), any());
    }

    @Test
    void shouldRejectGetWhenARunningRunExists() {
        stubActiveMerchant();
        stubInFlight();

        assertThatThrownBy(() -> periodService.get(merchantId, FROM, TO))
                .isInstanceOf(PeriodConflictException.class);
        verifyNoInteractions(discrepancyRepository);
    }

    @Test
    void shouldRejectGetWhenTheMerchantIsInactive() {
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> periodService.get(merchantId, FROM, TO))
                .isInstanceOf(MerchantNotFoundException.class);
        verifyNoInteractions(reconciliationRunRepository, periodLockRepository, periodGuard);
    }

    @Test
    void shouldLockACleanWindowAndAuditAfterFlush() {
        ReconciliationRunEntity run = completedRun(2, 1, 1);
        UUID lockId = UUID.randomUUID();
        when(periodGuard.lockMerchant(merchantId)).thenReturn(merchant);
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.empty());
        stubNoInFlight();
        stubCurrentRun(run);
        when(discrepancyRepository.findByRunIdWithItem(runId))
                .thenReturn(List.of(feeGap(DiscrepancyStatus.ACCEPTED)));
        when(periodLockRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            PeriodLockEntity saved = invocation.getArgument(0);
            saved.setId(lockId);
            return saved;
        });
        clearInvocations(periodGuard, periodLockRepository, reconciliationRunRepository, discrepancyRepository, auditLogger);

        PeriodResponseDTO response = periodService.lock(merchantId, FROM, TO);

        assertThat(response.runId()).isEqualTo(runId);
        assertThat(response.locked()).isTrue();
        assertThat(response.lockedAt()).isEqualTo(LOCKED_AT);
        assertThat(response.openAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(response.matchRate()).isEqualTo(new BigDecimal("0.5000"));
        assertThat(response.closeDurationSeconds()).isEqualTo(CLOSE_SECONDS);
        ArgumentCaptor<PeriodLockEntity> saved = ArgumentCaptor.forClass(PeriodLockEntity.class);
        ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);
        InOrder order = inOrder(periodGuard, periodLockRepository, reconciliationRunRepository, discrepancyRepository, auditLogger);
        order.verify(periodGuard).lockMerchant(merchantId);
        order.verify(periodLockRepository).findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO);
        order.verify(reconciliationRunRepository).existsByMerchant_IdAndFromDateAndToDateAndStatusIn(
                merchantId, FROM, TO, IN_FLIGHT);
        order.verify(reconciliationRunRepository).findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, FROM, TO, ReconciliationRunStatus.COMPLETED);
        order.verify(discrepancyRepository).findByRunIdWithItem(runId);
        order.verify(periodLockRepository).saveAndFlush(saved.capture());
        order.verify(auditLogger).record(eq("PERIOD_LOCKED"), eq("periodLock"), eq(lockId), detail.capture());
        assertThat(saved.getValue().getMerchant()).isSameAs(merchant);
        assertThat(saved.getValue().getFromDate()).isEqualTo(FROM);
        assertThat(saved.getValue().getToDate()).isEqualTo(TO);
        assertThat(saved.getValue().getRun()).isSameAs(run);
        assertThat(saved.getValue().getLockedAt()).isEqualTo(LOCKED_AT);
        assertThat(detail.getValue()).contains(
                merchantId.toString(), FROM.toString(), TO.toString(), runId.toString());
    }

    @Test
    void shouldStoreLockedAtTruncatedToMicroseconds() {
        Instant nanos = Instant.parse("2026-07-16T10:00:30.400000500Z");
        clock.set(nanos);
        ReconciliationRunEntity run = completedRun(1, 1, 0);
        when(periodGuard.lockMerchant(merchantId)).thenReturn(merchant);
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.empty());
        stubNoInFlight();
        stubCurrentRun(run);
        when(discrepancyRepository.findByRunIdWithItem(runId)).thenReturn(List.of());
        when(periodLockRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PeriodResponseDTO response = periodService.lock(merchantId, FROM, TO);

        Instant micros = nanos.truncatedTo(ChronoUnit.MICROS);
        ArgumentCaptor<PeriodLockEntity> saved = ArgumentCaptor.forClass(PeriodLockEntity.class);
        verify(periodLockRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getLockedAt()).isEqualTo(micros);
        assertThat(response.lockedAt()).isEqualTo(micros);
    }

    @Test
    void shouldLockARunWithZeroItemsAndNullMatchRate() {
        ReconciliationRunEntity run = completedRun(0, 0, 0);
        stubLockable(run, List.of());
        when(periodLockRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PeriodResponseDTO response = periodService.lock(merchantId, FROM, TO);

        assertThat(response.totalItems()).isZero();
        assertThat(response.matchRate()).isNull();
        assertThat(response.openAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(response.locked()).isTrue();
        assertThat(response.closeDurationSeconds()).isEqualTo(CLOSE_SECONDS);
    }

    @Test
    void shouldRejectLockWhenADiscrepancyIsOpenAndStoreNothing() {
        ReconciliationRunEntity run = completedRun(2, 1, 1);
        when(periodGuard.lockMerchant(merchantId)).thenReturn(merchant);
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.empty());
        stubNoInFlight();
        stubCurrentRun(run);
        ReconciliationDiscrepancyEntity open = new ReconciliationDiscrepancyEntity();
        open.setType(DiscrepancyType.STATUS_MISMATCH);
        open.setStatus(DiscrepancyStatus.OPEN);
        open.setReconciliationItem(new ReconciliationItemEntity());
        when(discrepancyRepository.findByRunIdWithItem(runId)).thenReturn(List.of(open));

        assertThatThrownBy(() -> periodService.lock(merchantId, FROM, TO))
                .isInstanceOf(PeriodConflictException.class);
        verify(periodLockRepository, never()).saveAndFlush(any());
        verifyNoInteractions(auditLogger);
    }

    @Test
    void shouldRejectLockWhenNoCurrentCompletedRunExistsAndStoreNothing() {
        when(periodGuard.lockMerchant(merchantId)).thenReturn(merchant);
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.empty());
        stubNoInFlight();
        when(reconciliationRunRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, FROM, TO, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> periodService.lock(merchantId, FROM, TO))
                .isInstanceOf(PeriodNotFoundException.class);
        verify(periodLockRepository, never()).saveAndFlush(any());
        verifyNoInteractions(auditLogger, discrepancyRepository);
    }

    @Test
    void shouldRejectLockWhenAPendingOrRunningRunExistsAndStoreNothing() {
        when(periodGuard.lockMerchant(merchantId)).thenReturn(merchant);
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.empty());
        stubInFlight();

        assertThatThrownBy(() -> periodService.lock(merchantId, FROM, TO))
                .isInstanceOf(PeriodConflictException.class);
        verify(periodLockRepository, never()).saveAndFlush(any());
        verify(reconciliationRunRepository, never())
                .findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                        any(), any(), any(), any());
        verifyNoInteractions(auditLogger);
    }

    @Test
    void shouldKeepTheOriginalLockedAtWhenLockingAgain() {
        ReconciliationRunEntity run = completedRun(2, 1, 1);
        PeriodLockEntity existing = lock(run, ORIGINAL_LOCKED_AT);
        when(periodGuard.lockMerchant(merchantId)).thenReturn(merchant);
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> periodService.lock(merchantId, FROM, TO))
                .isInstanceOf(PeriodConflictException.class);
        assertThat(existing.getLockedAt()).isEqualTo(ORIGINAL_LOCKED_AT);
        verify(periodLockRepository, never()).saveAndFlush(any());
        verify(periodLockRepository, never()).delete(any());
        verifyNoInteractions(auditLogger, reconciliationRunRepository);
    }

    @Test
    void shouldUnlockAndClearTheDurationWhileKeepingRunIdAndMatchRate() {
        ReconciliationRunEntity run = completedRun(2, 1, 1);
        PeriodLockEntity existing = lock(run, ORIGINAL_LOCKED_AT);
        existing.setId(UUID.randomUUID());
        when(periodGuard.lockMerchant(merchantId)).thenReturn(merchant);
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.of(existing));
        when(discrepancyRepository.findByRunIdWithItem(runId)).thenReturn(List.of());
        clearInvocations(periodLockRepository, auditLogger);

        PeriodResponseDTO response = periodService.unlock(merchantId, FROM, TO);

        assertThat(response.runId()).isEqualTo(runId);
        assertThat(response.matchRate()).isEqualTo(new BigDecimal("0.5000"));
        assertThat(response.locked()).isFalse();
        assertThat(response.lockedAt()).isNull();
        assertThat(response.closeDurationSeconds()).isNull();
        assertThat(response.openAmount()).isEqualTo(new BigDecimal("0.00"));
        ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);
        InOrder order = inOrder(periodLockRepository, auditLogger);
        order.verify(periodLockRepository).delete(existing);
        order.verify(periodLockRepository).flush();
        order.verify(auditLogger).record(eq("PERIOD_UNLOCKED"), eq("periodLock"), eq(existing.getId()), detail.capture());
        assertThat(detail.getValue()).contains(merchantId.toString(), FROM.toString(), TO.toString());
    }

    @Test
    void shouldRejectUnlockWhenTheWindowIsNotLocked() {
        when(periodGuard.lockMerchant(merchantId)).thenReturn(merchant);
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> periodService.unlock(merchantId, FROM, TO))
                .isInstanceOf(PeriodConflictException.class);
        verify(periodLockRepository, never()).delete(any());
        verify(periodLockRepository, never()).flush();
        verifyNoInteractions(auditLogger);
    }

    @Test
    void shouldMeasureTheRelockFromTheSameFinishedAt() {
        ReconciliationRunEntity run = completedRun(2, 1, 1);
        PeriodLockEntity existing = lock(run, ORIGINAL_LOCKED_AT);
        existing.setId(UUID.randomUUID());
        when(periodGuard.lockMerchant(merchantId)).thenReturn(merchant);
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.of(existing));
        when(discrepancyRepository.findByRunIdWithItem(runId)).thenReturn(List.of());

        PeriodResponseDTO unlocked = periodService.unlock(merchantId, FROM, TO);

        assertThat(unlocked.locked()).isFalse();
        assertThat(unlocked.closeDurationSeconds()).isNull();
        assertThat(unlocked.runId()).isEqualTo(runId);
        clock.set(LOCKED_AT);
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.empty());
        stubNoInFlight();
        stubCurrentRun(run);
        when(periodLockRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PeriodResponseDTO locked = periodService.lock(merchantId, FROM, TO);

        assertThat(locked.runId()).isEqualTo(runId);
        assertThat(locked.locked()).isTrue();
        assertThat(locked.lockedAt()).isEqualTo(LOCKED_AT);
        assertThat(locked.closeDurationSeconds()).isEqualTo(CLOSE_SECONDS);
        assertThat(locked.matchRate()).isEqualTo(unlocked.matchRate());
        assertThat(run.getFinishedAt()).isEqualTo(FINISHED_AT);
    }

    @Test
    void shouldNotAuditWhenSavingTheLockFails() {
        ReconciliationRunEntity run = completedRun(2, 2, 0);
        stubLockable(run, List.of());
        DataIntegrityViolationException failure = new DataIntegrityViolationException("uk_period_locks_window");
        doThrow(failure).when(periodLockRepository).saveAndFlush(any());

        assertThatThrownBy(() -> periodService.lock(merchantId, FROM, TO)).isSameAs(failure);
        verifyNoInteractions(auditLogger);
    }

    @Test
    void shouldRejectAnInvertedWindowBeforeLockingTheMerchant() {
        assertThatThrownBy(() -> periodService.lock(merchantId, TO, FROM))
                .isInstanceOf(InvalidReconciliationWindowException.class);
        assertThatThrownBy(() -> periodService.get(merchantId, TO, FROM))
                .isInstanceOf(InvalidReconciliationWindowException.class);
        assertThatThrownBy(() -> periodService.unlock(merchantId, TO, FROM))
                .isInstanceOf(InvalidReconciliationWindowException.class);
        verifyNoInteractions(periodGuard, merchantRepository, reconciliationRunRepository, periodLockRepository);
    }

    @Test
    void shouldRejectAWindowBeyondMaxDaysBeforeLockingTheMerchant() {
        LocalDate tooLate = LocalDate.parse("2027-01-02");

        assertThatThrownBy(() -> periodService.lock(merchantId, LocalDate.parse("2026-01-01"), tooLate))
                .isInstanceOf(InvalidReconciliationWindowException.class);
        verifyNoInteractions(periodGuard, reconciliationRunRepository, periodLockRepository);
    }

    private void stubReadableWindow(ReconciliationRunEntity run, PeriodLockEntity lock) {
        stubActiveMerchant();
        stubNoInFlight();
        stubCurrentRun(run);
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.ofNullable(lock));
        when(discrepancyRepository.findByRunIdWithItem(runId)).thenReturn(List.of());
    }

    private void stubLockable(ReconciliationRunEntity run, List<ReconciliationDiscrepancyEntity> discrepancies) {
        when(periodGuard.lockMerchant(merchantId)).thenReturn(merchant);
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.empty());
        stubNoInFlight();
        stubCurrentRun(run);
        when(discrepancyRepository.findByRunIdWithItem(runId)).thenReturn(discrepancies);
    }

    private void stubActiveMerchant() {
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
    }

    private void stubNoInFlight() {
        when(reconciliationRunRepository.existsByMerchant_IdAndFromDateAndToDateAndStatusIn(
                merchantId, FROM, TO, IN_FLIGHT)).thenReturn(false);
    }

    private void stubInFlight() {
        when(reconciliationRunRepository.existsByMerchant_IdAndFromDateAndToDateAndStatusIn(
                merchantId, FROM, TO, IN_FLIGHT)).thenReturn(true);
    }

    private void stubCurrentRun(ReconciliationRunEntity run) {
        when(reconciliationRunRepository.findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                merchantId, FROM, TO, ReconciliationRunStatus.COMPLETED))
                .thenReturn(Optional.of(run));
    }

    private ReconciliationRunEntity completedRun(int totalItems, int matchedCount, int divergentCount) {
        ReconciliationRunEntity run = new ReconciliationRunEntity();
        run.setId(runId);
        run.setMerchant(merchant);
        run.setFromDate(FROM);
        run.setToDate(TO);
        run.setStatus(ReconciliationRunStatus.COMPLETED);
        run.setSupersededAt(null);
        run.setTotalItems(totalItems);
        run.setMatchedCount(matchedCount);
        run.setDivergentCount(divergentCount);
        run.setFinishedAt(FINISHED_AT);
        return run;
    }

    private PeriodLockEntity lock(ReconciliationRunEntity run, Instant lockedAt) {
        PeriodLockEntity lock = new PeriodLockEntity();
        lock.setMerchant(merchant);
        lock.setFromDate(FROM);
        lock.setToDate(TO);
        lock.setRun(run);
        lock.setLockedAt(lockedAt);
        return lock;
    }

    private ReconciliationDiscrepancyEntity feeGap(DiscrepancyStatus status) {
        ReconciliationItemEntity item = new ReconciliationItemEntity();
        item.setExpectedNetAmount(new BigDecimal("100.00"));
        item.setSettlementNetAmount(new BigDecimal("97.20"));
        ReconciliationDiscrepancyEntity discrepancy = new ReconciliationDiscrepancyEntity();
        discrepancy.setType(DiscrepancyType.FEE_DIVERGENCE);
        discrepancy.setStatus(status);
        discrepancy.setReconciliationItem(item);
        return discrepancy;
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
