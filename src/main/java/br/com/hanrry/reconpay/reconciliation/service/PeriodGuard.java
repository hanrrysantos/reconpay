package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.PeriodConflictException;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.reconciliation.entity.PeriodLockEntity;
import br.com.hanrry.reconpay.reconciliation.repository.IPeriodLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PeriodGuard {

    private final IMerchantRepository merchantRepository;
    private final IPeriodLockRepository periodLockRepository;

    public MerchantEntity lockMerchant(UUID merchantId) {
        return merchantRepository.findByIdAndActiveTrueForUpdate(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));
    }

    public void assertWindowOpen(UUID merchantId, LocalDate fromDate, LocalDate toDate) {
        lockMerchant(merchantId);
        if (periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, fromDate, toDate).isPresent()) {
            throw new PeriodConflictException("Janela travada de " + fromDate + " a " + toDate);
        }
    }

    public void assertDatesOpen(UUID merchantId, Collection<LocalDate> dates) {
        lockMerchant(merchantId);
        for (PeriodLockEntity lock : periodLockRepository.findByMerchant_Id(merchantId)) {
            for (LocalDate date : dates) {
                if (!date.isBefore(lock.getFromDate()) && !date.isAfter(lock.getToDate())) {
                    throw new PeriodConflictException(
                            "Janela travada de " + lock.getFromDate() + " a " + lock.getToDate());
                }
            }
        }
    }
}
