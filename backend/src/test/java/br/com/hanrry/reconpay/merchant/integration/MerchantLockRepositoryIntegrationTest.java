package br.com.hanrry.reconpay.merchant.integration;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MerchantLockRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private IMerchantRepository merchantRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReturnTheActiveMerchant() {
        MerchantEntity merchant = saveMerchant("Active lock", true);

        Optional<MerchantEntity> found = merchantRepository.findByIdAndActiveTrueForUpdate(merchant.getId());

        assertThat(found).isPresent();
        MerchantEntity locked = found.orElseThrow();
        assertThat(locked.getId()).isEqualTo(merchant.getId());
        assertThat(locked.getName()).isEqualTo("Active lock");
        assertThat(locked.isActive()).isTrue();
    }

    @Test
    void shouldReturnEmptyWhenTheMerchantIsInactiveOrMissing() {
        MerchantEntity inactive = saveMerchant("Inactive lock", false);

        assertThat(merchantRepository.findById(inactive.getId()).orElseThrow().isActive()).isFalse();
        assertThat(merchantRepository.findByIdAndActiveTrueForUpdate(inactive.getId())).isEmpty();
        assertThat(merchantRepository.findByIdAndActiveTrueForUpdate(UUID.randomUUID())).isEmpty();
    }

    @Test
    void shouldHoldTheRowLockUntilTheTransactionEnds() throws Exception {
        UUID merchantId = saveMerchant("Contended lock", true).getId();
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Optional<MerchantEntity>> holder = executor.submit(() ->
                new TransactionTemplate(transactionManager).execute(status -> {
                    Optional<MerchantEntity> locked = merchantRepository.findByIdAndActiveTrueForUpdate(merchantId);
                    held.countDown();
                    try {
                        if (!release.await(15, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("lock holder timed out");
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(ex);
                    }
                    return locked;
                }));

        try {
            assertThat(held.await(15, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                jdbcTemplate.queryForObject("SELECT set_config('lock_timeout', '300ms', true)", String.class);
                merchantRepository.findByIdAndActiveTrueForUpdate(merchantId);
            })).isInstanceOf(PessimisticLockingFailureException.class);
        } finally {
            release.countDown();
            executor.shutdown();
        }

        Optional<MerchantEntity> locked = holder.get(15, TimeUnit.SECONDS);
        assertThat(locked).isPresent();
        assertThat(locked.orElseThrow().getId()).isEqualTo(merchantId);
        assertThat(locked.orElseThrow().isActive()).isTrue();
    }

    private MerchantEntity saveMerchant(String name, boolean active) {
        MerchantEntity merchant = new MerchantEntity();
        merchant.setName(name);
        merchant.setDocument(UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        merchant = merchantRepository.saveAndFlush(merchant);
        if (!active) {
            merchant.setActive(false);
            merchant = merchantRepository.saveAndFlush(merchant);
        }
        return merchant;
    }
}
