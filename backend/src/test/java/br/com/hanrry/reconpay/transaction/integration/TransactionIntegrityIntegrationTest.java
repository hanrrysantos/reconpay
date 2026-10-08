package br.com.hanrry.reconpay.transaction.integration;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class TransactionIntegrityIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired IMerchantRepository merchants;
    UUID merchantId;
    @Autowired br.com.hanrry.reconpay.transaction.repository.IInternalTransactionRepository transactions;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired br.com.hanrry.reconpay.transaction.service.TransactionService service;

    @Test void competingTerminalTransitionsCannotBothCommit() throws Exception {
        UUID id = insertTransaction("99.00");
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("reconpay.audit");
        var events = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        events.list = new java.util.concurrent.CopyOnWriteArrayList<>();
        events.start();
        logger.addAppender(events);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var refund = executor.submit(() -> transition(id, br.com.hanrry.reconpay.transaction.enums.TransactionStatus.REFUNDED, barrier));
            var chargeback = executor.submit(() -> transition(id, br.com.hanrry.reconpay.transaction.enums.TransactionStatus.CHARGEBACK, barrier));
            assertThat(java.util.List.of(refund.get(15, java.util.concurrent.TimeUnit.SECONDS), chargeback.get(15, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(events.list.stream().filter(event -> event.getFormattedMessage().contains(id.toString())
                    && event.getFormattedMessage().contains("TRANSACTION_STATUS_CHANGED"))).hasSize(1);
        } finally {
            logger.detachAppender(events);
            events.stop();
        }
    }

    private boolean transition(UUID id, br.com.hanrry.reconpay.transaction.enums.TransactionStatus status, java.util.concurrent.CyclicBarrier barrier) {
        try {
            new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                assertThat(transactions.findById(id).orElseThrow().getStatus()).isEqualTo(br.com.hanrry.reconpay.transaction.enums.TransactionStatus.APPROVED);
                try { barrier.await(10, java.util.concurrent.TimeUnit.SECONDS); }
                catch (Exception ex) { throw new IllegalStateException(ex); }
                service.updateStatus(merchantId, id, new br.com.hanrry.reconpay.transaction.dto.UpdateTransactionStatusRequestDTO(status));
            });
            return true;
        } catch (org.springframework.dao.OptimisticLockingFailureException ex) {
            return false;
        }
    }

    @BeforeEach void merchant() {
        var merchant = new MerchantEntity();
        merchant.setName("Integrity");
        merchant.setDocument(UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        merchant.setActive(true);
        merchantId = merchants.saveAndFlush(merchant).getId();
    }

    @Test void databaseRejectsInvalidFees() {
        assertThatThrownBy(() -> insertFee("100.0001", "0.00")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertFee("0.00", "-0.01")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertFee("-0.0001", "0.00")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test void databaseRejectsNonPositiveExpectedNet() {
        assertThatThrownBy(() -> insertTransaction("0.00")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertTransaction("-0.01")).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insertFee(String percent, String fixed) {
        jdbc.update("insert into fee_rules(id, merchant_id, payment_method, installments, fee_percentage, fixed_fee, active, created_at, updated_at) values (?, ?, 'PIX', 1, ?::numeric, ?::numeric, true, now(), now())", UUID.randomUUID(), merchantId, percent, fixed);
    }

    UUID insertTransaction(String net) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into internal_transactions(id, merchant_id, external_reference, amount, expected_net_amount, payment_method, installments, status, transaction_date, created_at, updated_at) values (?, ?, ?, 100.00, ?::numeric, 'PIX', 1, 'APPROVED', '2026-01-01', now(), now())", id, merchantId, id.toString(), net);
        return id;
    }
}
