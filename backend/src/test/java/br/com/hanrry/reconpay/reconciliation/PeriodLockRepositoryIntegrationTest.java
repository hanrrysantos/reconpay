package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.reconciliation.entity.PeriodLockEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IPeriodLockRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodLockRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate FROM = LocalDate.parse("2026-07-01");
    private static final LocalDate TO = LocalDate.parse("2026-07-15");
    private static final Instant LOCKED_AT = Instant.parse("2026-07-16T15:00:00Z");
    private static final Instant LATER_LOCKED_AT = Instant.parse("2026-07-16T16:00:00Z");

    @Autowired
    private IPeriodLockRepository periodLockRepository;

    @Autowired
    private IMerchantRepository merchantRepository;

    @Autowired
    private IReconciliationRunRepository reconciliationRunRepository;

    @Test
    @Transactional
    void shouldRereadTheLockByExactWindowAndByMerchant() {
        MerchantEntity merchant = saveMerchant("Period A");
        MerchantEntity otherMerchant = saveMerchant("Period B");
        ReconciliationRunEntity run = saveRun(merchant, FROM, TO);
        ReconciliationRunEntity otherRun = saveRun(otherMerchant, FROM, TO);
        PeriodLockEntity lock = saveLock(merchant, run, FROM, TO, LOCKED_AT);
        LocalDate laterFrom = LocalDate.parse("2026-07-16");
        LocalDate laterTo = LocalDate.parse("2026-07-31");
        PeriodLockEntity laterLock = saveLock(
                merchant,
                saveRun(merchant, laterFrom, laterTo),
                laterFrom,
                laterTo,
                LATER_LOCKED_AT);
        PeriodLockEntity otherLock = saveLock(otherMerchant, otherRun, FROM, TO, LOCKED_AT);

        Optional<PeriodLockEntity> found = periodLockRepository
                .findByMerchant_IdAndFromDateAndToDate(merchant.getId(), FROM, TO);

        assertThat(found).isPresent();
        PeriodLockEntity stored = found.orElseThrow();
        assertThat(stored.getId()).isEqualTo(lock.getId());
        assertThat(stored.getMerchant().getId()).isEqualTo(merchant.getId());
        assertThat(stored.getFromDate()).isEqualTo(FROM);
        assertThat(stored.getToDate()).isEqualTo(TO);
        assertThat(stored.getRun().getId()).isEqualTo(run.getId());
        assertThat(stored.getLockedAt()).isEqualTo(LOCKED_AT);

        assertThat(periodLockRepository.findByMerchant_IdAndFromDateAndToDate(
                merchant.getId(), FROM, laterTo)).isEmpty();

        assertThat(periodLockRepository.findByMerchant_Id(merchant.getId()))
                .extracting(PeriodLockEntity::getId)
                .containsExactlyInAnyOrder(lock.getId(), laterLock.getId());

        assertThat(periodLockRepository.findByMerchant_Id(otherMerchant.getId()))
                .extracting(PeriodLockEntity::getId)
                .containsExactly(otherLock.getId());
    }

    @Test
    void shouldRejectASecondLockForTheSameWindow() {
        MerchantEntity merchant = saveMerchant("Period Unique");
        ReconciliationRunEntity run = saveRun(merchant, FROM, TO);
        PeriodLockEntity first = saveLock(merchant, run, FROM, TO, LOCKED_AT);

        PeriodLockEntity duplicate = new PeriodLockEntity();
        duplicate.setMerchant(merchant);
        duplicate.setRun(run);
        duplicate.setFromDate(FROM);
        duplicate.setToDate(TO);
        duplicate.setLockedAt(LATER_LOCKED_AT);

        assertThatThrownBy(() -> periodLockRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_period_locks_window");

        PeriodLockEntity stored = periodLockRepository
                .findByMerchant_IdAndFromDateAndToDate(merchant.getId(), FROM, TO)
                .orElseThrow();
        assertThat(stored.getId()).isEqualTo(first.getId());
        assertThat(stored.getLockedAt()).isEqualTo(LOCKED_AT);
        assertThat(periodLockRepository.findByMerchant_Id(merchant.getId())).hasSize(1);
    }

    private MerchantEntity saveMerchant(String name) {
        MerchantEntity merchant = new MerchantEntity();
        merchant.setName(name);
        merchant.setDocument(UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        return merchantRepository.saveAndFlush(merchant);
    }

    private ReconciliationRunEntity saveRun(MerchantEntity merchant, LocalDate fromDate, LocalDate toDate) {
        ReconciliationRunEntity run = new ReconciliationRunEntity();
        run.setMerchant(merchant);
        run.setFromDate(fromDate);
        run.setToDate(toDate);
        run.setTotalItems(0);
        run.setMatchedCount(0);
        run.setDivergentCount(0);
        run.setStatus(ReconciliationRunStatus.COMPLETED);
        return reconciliationRunRepository.saveAndFlush(run);
    }

    private PeriodLockEntity saveLock(
            MerchantEntity merchant,
            ReconciliationRunEntity run,
            LocalDate fromDate,
            LocalDate toDate,
            Instant lockedAt) {
        PeriodLockEntity lock = new PeriodLockEntity();
        lock.setMerchant(merchant);
        lock.setRun(run);
        lock.setFromDate(fromDate);
        lock.setToDate(toDate);
        lock.setLockedAt(lockedAt);
        return periodLockRepository.saveAndFlush(lock);
    }
}
