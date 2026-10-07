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
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IPeriodLockRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationDiscrepancyRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PeriodService {

    private static final List<ReconciliationRunStatus> IN_FLIGHT = List.of(
            ReconciliationRunStatus.PENDING,
            ReconciliationRunStatus.RUNNING);

    private final PeriodGuard periodGuard;
    private final OpenAmountCalculator openAmountCalculator;
    private final IReconciliationRunRepository reconciliationRunRepository;
    private final IPeriodLockRepository periodLockRepository;
    private final IReconciliationDiscrepancyRepository discrepancyRepository;
    private final IMerchantRepository merchantRepository;
    private final ReconciliationProperties properties;
    private final AuditLogger auditLogger;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PeriodResponseDTO get(UUID merchantId, LocalDate fromDate, LocalDate toDate) {
        ensureWindow(fromDate, toDate);
        merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));
        rejectInFlight(merchantId, fromDate, toDate);
        ReconciliationRunEntity run = currentCompleted(merchantId, fromDate, toDate);
        PeriodLockEntity lock = periodLockRepository
                .findByMerchant_IdAndFromDateAndToDate(merchantId, fromDate, toDate)
                .orElse(null);
        return toResponse(run, lock);
    }

    @Transactional
    public PeriodResponseDTO lock(UUID merchantId, LocalDate fromDate, LocalDate toDate) {
        ensureWindow(fromDate, toDate);
        MerchantEntity merchant = periodGuard.lockMerchant(merchantId);
        if (periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, fromDate, toDate).isPresent()) {
            throw new PeriodConflictException("Janela já travada de " + fromDate + " a " + toDate);
        }
        rejectInFlight(merchantId, fromDate, toDate);
        ReconciliationRunEntity run = currentCompleted(merchantId, fromDate, toDate);
        List<ReconciliationDiscrepancyEntity> discrepancies = discrepancyRepository.findByRunIdWithItem(run.getId());
        if (discrepancies.stream().anyMatch(discrepancy -> discrepancy.getStatus() == DiscrepancyStatus.OPEN)) {
            throw new PeriodConflictException("Janela possui divergência em aberto");
        }

        PeriodLockEntity lock = new PeriodLockEntity();
        lock.setMerchant(merchant);
        lock.setFromDate(fromDate);
        lock.setToDate(toDate);
        lock.setRun(run);
        lock.setLockedAt(clock.instant());
        PeriodLockEntity saved = periodLockRepository.saveAndFlush(lock);
        auditLogger.record(
                "PERIOD_LOCKED",
                "periodLock",
                saved.getId(),
                "merchantId=" + merchantId + " fromDate=" + fromDate + " toDate=" + toDate + " runId=" + run.getId());
        return toResponse(run, saved, discrepancies);
    }

    @Transactional
    public PeriodResponseDTO unlock(UUID merchantId, LocalDate fromDate, LocalDate toDate) {
        ensureWindow(fromDate, toDate);
        periodGuard.lockMerchant(merchantId);
        PeriodLockEntity lock = periodLockRepository
                .findByMerchant_IdAndFromDateAndToDate(merchantId, fromDate, toDate)
                .orElseThrow(() -> new PeriodConflictException(
                        "Janela não está travada de " + fromDate + " a " + toDate));
        ReconciliationRunEntity run = lock.getRun();
        periodLockRepository.delete(lock);
        periodLockRepository.flush();
        auditLogger.record(
                "PERIOD_UNLOCKED",
                "periodLock",
                lock.getId(),
                "merchantId=" + merchantId + " fromDate=" + fromDate + " toDate=" + toDate);
        return toResponse(run, null);
    }

    private void ensureWindow(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new InvalidReconciliationWindowException("fromDate deve ser anterior ou igual a toDate");
        }
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (days > properties.maxWindowDays()) {
            throw new InvalidReconciliationWindowException(
                    "Janela de conciliação excede o máximo de " + properties.maxWindowDays() + " dias");
        }
    }

    private void rejectInFlight(UUID merchantId, LocalDate fromDate, LocalDate toDate) {
        if (reconciliationRunRepository.existsByMerchant_IdAndFromDateAndToDateAndStatusIn(
                merchantId, fromDate, toDate, IN_FLIGHT)) {
            throw new PeriodConflictException(
                    "Janela possui conciliação em andamento de " + fromDate + " a " + toDate);
        }
    }

    private ReconciliationRunEntity currentCompleted(UUID merchantId, LocalDate fromDate, LocalDate toDate) {
        return reconciliationRunRepository
                .findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                        merchantId, fromDate, toDate, ReconciliationRunStatus.COMPLETED)
                .orElseThrow(() -> new PeriodNotFoundException(
                        "Período não encontrado de " + fromDate + " a " + toDate));
    }

    private PeriodResponseDTO toResponse(ReconciliationRunEntity run, PeriodLockEntity lock) {
        return toResponse(run, lock, discrepancyRepository.findByRunIdWithItem(run.getId()));
    }

    private PeriodResponseDTO toResponse(
            ReconciliationRunEntity run,
            PeriodLockEntity lock,
            List<ReconciliationDiscrepancyEntity> discrepancies) {
        return new PeriodResponseDTO(
                run.getId(),
                run.getFromDate(),
                run.getToDate(),
                run.getTotalItems(),
                run.getMatchedCount(),
                run.getDivergentCount(),
                matchRate(run),
                openAmountCalculator.sum(discrepancies),
                lock != null,
                lock == null ? null : lock.getLockedAt(),
                closeDuration(run, lock));
    }

    private BigDecimal matchRate(ReconciliationRunEntity run) {
        if (run.getTotalItems() == 0) {
            return null;
        }
        return BigDecimal.valueOf(run.getMatchedCount())
                .divide(BigDecimal.valueOf(run.getTotalItems()), 4, RoundingMode.HALF_UP);
    }

    private Long closeDuration(ReconciliationRunEntity run, PeriodLockEntity lock) {
        if (lock == null) {
            return null;
        }
        return Duration.between(run.getFinishedAt(), lock.getLockedAt()).toSeconds();
    }
}
