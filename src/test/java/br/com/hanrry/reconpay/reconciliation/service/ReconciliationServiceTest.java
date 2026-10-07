package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.exception.InvalidReconciliationWindowException;
import br.com.hanrry.reconpay.exception.PeriodConflictException;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.reconciliation.dto.ReconciliationRunResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.RunReconciliationRequestDTO;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.mapper.IReconciliationMapper;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationItemRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    private static final LocalDate FROM = LocalDate.parse("2026-07-01");
    private static final LocalDate TO = LocalDate.parse("2026-07-15");

    @Mock
    private ReconciliationCsvExporter reconciliationCsvExporter;

    @Mock
    private IReconciliationMapper reconciliationMapper;

    @Mock
    private IReconciliationRunRepository reconciliationRunRepository;

    @Mock
    private IReconciliationItemRepository reconciliationItemRepository;

    @Mock
    private IMerchantRepository merchantRepository;

    @Mock
    private PeriodGuard periodGuard;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private EntityManager entityManager;

    private ReconciliationService reconciliationService;
    private UUID merchantId;
    private MerchantEntity merchant;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        merchant = new MerchantEntity();
        merchant.setId(merchantId);
        merchant.setActive(true);
        reconciliationService = new ReconciliationService(
                reconciliationCsvExporter,
                reconciliationMapper,
                reconciliationRunRepository,
                reconciliationItemRepository,
                merchantRepository,
                periodGuard,
                new ReconciliationProperties(new BigDecimal("0.00"), 5, 366, true, 2, 50),
                auditLogger,
                entityManager);
    }

    @Test
    void shouldRejectARunOnTheLockedWindowAndStoreNothing() {
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        doThrow(new PeriodConflictException("Janela travada de " + FROM + " a " + TO))
                .when(periodGuard).assertWindowOpen(merchantId, FROM, TO);

        assertThatThrownBy(() -> reconciliationService.run(merchantId, new RunReconciliationRequestDTO(FROM, TO)))
                .isInstanceOf(PeriodConflictException.class);

        verify(reconciliationRunRepository, never()).saveAndFlush(any());
        verifyNoInteractions(auditLogger);
    }

    @Test
    void shouldStillSavePendingWhenTheWindowDiffersFromTheLockedPair() {
        LocalDate overlappingFrom = LocalDate.parse("2026-07-10");
        LocalDate overlappingTo = LocalDate.parse("2026-07-20");
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(reconciliationRunRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ReconciliationRunEntity saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(reconciliationMapper.toRunDTO(any())).thenAnswer(invocation -> {
            ReconciliationRunEntity saved = invocation.getArgument(0);
            return new ReconciliationRunResponseDTO(
                    saved.getId(),
                    merchantId,
                    saved.getFromDate(),
                    saved.getToDate(),
                    saved.getStatus(),
                    saved.getTotalItems(),
                    saved.getMatchedCount(),
                    saved.getDivergentCount(),
                    saved.getCreatedAt(),
                    saved.getStartedAt(),
                    saved.getFinishedAt(),
                    saved.getErrorMessage(),
                    saved.getSupersededAt());
        });

        ReconciliationRunResponseDTO response = reconciliationService.run(
                merchantId, new RunReconciliationRequestDTO(overlappingFrom, overlappingTo));

        ArgumentCaptor<ReconciliationRunEntity> saved = ArgumentCaptor.forClass(ReconciliationRunEntity.class);
        InOrder order = inOrder(periodGuard, reconciliationRunRepository);
        order.verify(periodGuard).assertWindowOpen(merchantId, overlappingFrom, overlappingTo);
        order.verify(reconciliationRunRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(ReconciliationRunStatus.PENDING);
        assertThat(saved.getValue().getFromDate()).isEqualTo(overlappingFrom);
        assertThat(saved.getValue().getToDate()).isEqualTo(overlappingTo);
        assertThat(saved.getValue().getMerchant()).isSameAs(merchant);
        assertThat(response.status()).isEqualTo(ReconciliationRunStatus.PENDING);
        assertThat(response.fromDate()).isEqualTo(overlappingFrom);
        assertThat(response.toDate()).isEqualTo(overlappingTo);
    }

    @Test
    void shouldRejectAWindowBeyondMaxDaysBeforeConsultingTheGuard() {
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> reconciliationService.run(
                merchantId,
                new RunReconciliationRequestDTO(LocalDate.parse("2020-01-01"), LocalDate.parse("2026-12-31"))))
                .isInstanceOf(InvalidReconciliationWindowException.class);

        verifyNoInteractions(periodGuard);
        verify(reconciliationRunRepository, never()).saveAndFlush(any());
    }
}
