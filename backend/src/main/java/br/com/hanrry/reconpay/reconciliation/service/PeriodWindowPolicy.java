package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.exception.InvalidReconciliationWindowException;
import br.com.hanrry.reconpay.exception.PeriodConflictException;
import br.com.hanrry.reconpay.exception.PeriodNotFoundException;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/*
 * The window rules every period read shares: the same request answers 400,
 * 404 or 409 whether it asks for the period or for its summary.
 */
@Component
@RequiredArgsConstructor
public class PeriodWindowPolicy {

    private static final List<ReconciliationRunStatus> IN_FLIGHT = List.of(
            ReconciliationRunStatus.PENDING,
            ReconciliationRunStatus.RUNNING);

    private final IReconciliationRunRepository reconciliationRunRepository;
    private final ReconciliationProperties properties;

    public void ensureWindow(LocalDate fromDate, LocalDate toDate) {
        if (fromDate.isAfter(toDate)) {
            throw new InvalidReconciliationWindowException("fromDate deve ser anterior ou igual a toDate");
        }
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        if (days > properties.maxWindowDays()) {
            throw new InvalidReconciliationWindowException(
                    "Janela de conciliação excede o máximo de " + properties.maxWindowDays() + " dias");
        }
    }

    public void rejectInFlight(UUID merchantId, LocalDate fromDate, LocalDate toDate) {
        if (reconciliationRunRepository.existsByMerchant_IdAndFromDateAndToDateAndStatusIn(
                merchantId, fromDate, toDate, IN_FLIGHT)) {
            throw new PeriodConflictException(
                    "Janela possui conciliação em andamento de " + fromDate + " a " + toDate);
        }
    }

    public ReconciliationRunEntity currentCompleted(UUID merchantId, LocalDate fromDate, LocalDate toDate) {
        return reconciliationRunRepository
                .findByMerchant_IdAndFromDateAndToDateAndStatusAndSupersededAtIsNull(
                        merchantId, fromDate, toDate, ReconciliationRunStatus.COMPLETED)
                .orElseThrow(() -> new PeriodNotFoundException(
                        "Período não encontrado de " + fromDate + " a " + toDate));
    }
}
