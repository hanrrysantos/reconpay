package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.PeriodConflictException;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.reconciliation.entity.PeriodLockEntity;
import br.com.hanrry.reconpay.reconciliation.repository.IPeriodLockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodGuardTest {

    private static final LocalDate FROM = LocalDate.parse("2026-07-01");
    private static final LocalDate TO = LocalDate.parse("2026-07-15");
    private static final LocalDate LATER_FROM = LocalDate.parse("2026-08-01");
    private static final LocalDate LATER_TO = LocalDate.parse("2026-08-15");

    @Mock
    private IMerchantRepository merchantRepository;

    @Mock
    private IPeriodLockRepository periodLockRepository;

    @InjectMocks
    private PeriodGuard periodGuard;

    private UUID merchantId;
    private MerchantEntity merchant;

    @BeforeEach
    void activeMerchant() {
        merchantId = UUID.randomUUID();
        merchant = new MerchantEntity();
        merchant.setId(merchantId);
        merchant.setName("Locked merchant");
        merchant.setActive(true);
    }

    @Test
    void shouldRejectAnExactLockedWindowAfterLockingTheMerchant() {
        when(merchantRepository.findByIdAndActiveTrueForUpdate(merchantId)).thenReturn(Optional.of(merchant));
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO))
                .thenReturn(Optional.of(lock(FROM, TO)));

        assertThatThrownBy(() -> periodGuard.assertWindowOpen(merchantId, FROM, TO))
                .isInstanceOf(PeriodConflictException.class);

        InOrder order = inOrder(merchantRepository, periodLockRepository);
        order.verify(merchantRepository).findByIdAndActiveTrueForUpdate(merchantId);
        order.verify(periodLockRepository).findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO);
        verify(periodLockRepository, never()).findByMerchant_Id(any());
    }

    @Test
    void shouldAllowAWindowThatHasNoLockRow() {
        when(merchantRepository.findByIdAndActiveTrueForUpdate(merchantId)).thenReturn(Optional.of(merchant));
        when(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO.plusDays(1)))
                .thenReturn(Optional.empty());

        assertThat(periodGuard.lockMerchant(merchantId).getId()).isEqualTo(merchantId);
        assertThat(periodGuard.lockMerchant(merchantId).isActive()).isTrue();
        periodGuard.assertWindowOpen(merchantId, FROM, TO.plusDays(1));

        InOrder order = inOrder(merchantRepository, periodLockRepository);
        order.verify(merchantRepository, times(3)).findByIdAndActiveTrueForUpdate(merchantId);
        order.verify(periodLockRepository).findByMerchant_IdAndFromDateAndToDate(merchantId, FROM, TO.plusDays(1));
    }

    @Test
    void shouldRejectADateEqualToTheLockStart() {
        when(merchantRepository.findByIdAndActiveTrueForUpdate(merchantId)).thenReturn(Optional.of(merchant));
        when(periodLockRepository.findByMerchant_Id(merchantId)).thenReturn(List.of(lock(FROM, TO)));

        assertThatThrownBy(() -> periodGuard.assertDatesOpen(merchantId, List.of(TO.plusDays(1), FROM)))
                .isInstanceOf(PeriodConflictException.class);

        InOrder order = inOrder(merchantRepository, periodLockRepository);
        order.verify(merchantRepository).findByIdAndActiveTrueForUpdate(merchantId);
        order.verify(periodLockRepository).findByMerchant_Id(merchantId);
        verify(periodLockRepository, never()).findByMerchant_IdAndFromDateAndToDate(any(), any(), any());
    }

    @Test
    void shouldRejectADateEqualToTheLockEnd() {
        when(merchantRepository.findByIdAndActiveTrueForUpdate(merchantId)).thenReturn(Optional.of(merchant));
        when(periodLockRepository.findByMerchant_Id(merchantId)).thenReturn(List.of(lock(FROM, TO)));

        assertThatThrownBy(() -> periodGuard.assertDatesOpen(merchantId, List.of(FROM.minusDays(1), TO)))
                .isInstanceOf(PeriodConflictException.class);

        InOrder order = inOrder(merchantRepository, periodLockRepository);
        order.verify(merchantRepository).findByIdAndActiveTrueForUpdate(merchantId);
        order.verify(periodLockRepository).findByMerchant_Id(merchantId);
    }

    @Test
    void shouldRejectADateInsideALaterLock() {
        when(merchantRepository.findByIdAndActiveTrueForUpdate(merchantId)).thenReturn(Optional.of(merchant));
        when(periodLockRepository.findByMerchant_Id(merchantId))
                .thenReturn(List.of(lock(FROM, TO), lock(LATER_FROM, LATER_TO)));

        assertThatThrownBy(() -> periodGuard.assertDatesOpen(
                merchantId,
                List.of(LocalDate.parse("2026-08-10"))))
                .isInstanceOf(PeriodConflictException.class);
    }

    @Test
    void shouldAllowADateOutsideEveryLock() {
        when(merchantRepository.findByIdAndActiveTrueForUpdate(merchantId)).thenReturn(Optional.of(merchant));
        when(periodLockRepository.findByMerchant_Id(merchantId))
                .thenReturn(List.of(lock(FROM, TO), lock(LATER_FROM, LATER_TO)));

        periodGuard.assertDatesOpen(merchantId, List.of(FROM.minusDays(1), TO.plusDays(1), LATER_TO.plusDays(1)));

        InOrder order = inOrder(merchantRepository, periodLockRepository);
        order.verify(merchantRepository).findByIdAndActiveTrueForUpdate(merchantId);
        order.verify(periodLockRepository).findByMerchant_Id(merchantId);
    }

    @Test
    void shouldRejectAnInactiveMerchantBeforeReadingLocks() {
        when(merchantRepository.findByIdAndActiveTrueForUpdate(merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> periodGuard.lockMerchant(merchantId))
                .isInstanceOf(MerchantNotFoundException.class)
                .hasMessage("Comerciante não encontrado com id: " + merchantId);
        assertThatThrownBy(() -> periodGuard.assertWindowOpen(merchantId, FROM, TO))
                .isInstanceOf(MerchantNotFoundException.class)
                .hasMessage("Comerciante não encontrado com id: " + merchantId);
        assertThatThrownBy(() -> periodGuard.assertDatesOpen(merchantId, List.of(FROM, TO)))
                .isInstanceOf(MerchantNotFoundException.class)
                .hasMessage("Comerciante não encontrado com id: " + merchantId);

        verify(merchantRepository, times(3)).findByIdAndActiveTrueForUpdate(merchantId);
        verifyNoInteractions(periodLockRepository);
    }

    private PeriodLockEntity lock(LocalDate fromDate, LocalDate toDate) {
        PeriodLockEntity lock = new PeriodLockEntity();
        lock.setFromDate(fromDate);
        lock.setToDate(toDate);
        return lock;
    }
}
