package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.bankstatement.repository.IBankStatementLineRepository;
import br.com.hanrry.reconpay.externalsettlement.repository.IExternalSettlementRepository;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.transaction.repository.IInternalTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CAP-01: the window is refused only above the limit, with both numbers in pt-BR. */
@ExtendWith(MockitoExtension.class)
class ReconciliationCapacityGuardTest {

    private static final LocalDate FROM = LocalDate.parse("2026-07-01");
    private static final LocalDate TO = LocalDate.parse("2026-07-31");

    @Mock
    private IInternalTransactionRepository transactionRepository;

    @Mock
    private IExternalSettlementRepository externalSettlementRepository;

    @Mock
    private IBankStatementLineRepository bankStatementLineRepository;

    private final UUID merchantId = UUID.randomUUID();
    private ReconciliationCapacityGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ReconciliationCapacityGuard(
                transactionRepository,
                externalSettlementRepository,
                bankStatementLineRepository,
                new ReconciliationProperties(new BigDecimal("0.00"), 5, 366, true, 2, 50, 300_000));
    }

    @Test
    void aWindowExactlyAtTheLimitIsAccepted() {
        records(100_000, 100_000, 100_000);

        assertThat(guard.rejectionFor(merchantId, FROM, TO)).isEmpty();
    }

    @Test
    void anEmptyWindowIsAccepted() {
        records(0, 0, 0);

        assertThat(guard.rejectionFor(merchantId, FROM, TO)).isEmpty();
    }

    @Test
    void oneRecordOverTheLimitIsRefusedWithTheTotalAndTheLimitInPtBr() {
        records(100_000, 100_000, 100_001);

        assertThat(guard.rejectionFor(merchantId, FROM, TO)).contains(
                "A janela tem 300.001 registros para conciliar; o limite é 300.000. Divida o período em janelas menores.");
    }

    @Test
    void everySourceCountsTowardTheTotal() {
        records(412_300, 0, 0);
        assertThat(guard.rejectionFor(merchantId, FROM, TO)).contains(
                "A janela tem 412.300 registros para conciliar; o limite é 300.000. Divida o período em janelas menores.");

        records(0, 412_300, 0);
        assertThat(guard.rejectionFor(merchantId, FROM, TO)).isPresent();

        records(0, 0, 412_300);
        assertThat(guard.rejectionFor(merchantId, FROM, TO)).isPresent();
    }

    @Test
    void bankLinesAreCountedOverTheWindowExtendedBySettlementLag() {
        records(0, 0, 0);

        guard.rejectionFor(merchantId, FROM, TO);

        verify(bankStatementLineRepository).countByMerchant_IdAndMovementDateBetween(merchantId, FROM, TO.plusDays(5));
    }

    @SuppressWarnings("unchecked")
    private void records(long transactions, long settlements, long bankLines) {
        when(transactionRepository.count(any(Specification.class))).thenReturn(transactions);
        when(externalSettlementRepository.count(any(Specification.class))).thenReturn(settlements);
        when(bankStatementLineRepository.countByMerchant_IdAndMovementDateBetween(merchantId, FROM, TO.plusDays(5)))
                .thenReturn(bankLines);
    }
}
