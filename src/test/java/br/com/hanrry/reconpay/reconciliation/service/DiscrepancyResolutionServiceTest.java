package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.auth.repository.IUserMerchantAccessRepository;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.exception.DiscrepancyNotFoundException;
import br.com.hanrry.reconpay.exception.DiscrepancyResolutionConflictException;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.reconciliation.dto.DiscrepancyDetailResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.UpdateDiscrepancyStatusRequestDTO;
import br.com.hanrry.reconpay.reconciliation.entity.DiscrepancyAdjustmentEntity;
import br.com.hanrry.reconpay.reconciliation.entity.DiscrepancyTransitionEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IDiscrepancyAdjustmentRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IDiscrepancyTransitionRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationDiscrepancyRepository;
import br.com.hanrry.reconpay.security.CustomUserDetails;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscrepancyResolutionServiceTest {

    @Mock
    private IReconciliationDiscrepancyRepository discrepancyRepository;

    @Mock
    private IDiscrepancyTransitionRepository transitionRepository;

    @Mock
    private IDiscrepancyAdjustmentRepository adjustmentRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private IUserMerchantAccessRepository userMerchantAccessRepository;

    @InjectMocks
    private DiscrepancyResolutionService service;

    private UUID actorId;
    private UserEntity actor;

    @BeforeEach
    void authenticateActor() {
        actorId = UUID.randomUUID();
        actor = new UserEntity();
        actor.setId(actorId);
        actor.setName("Admin");
        actor.setEmail("admin@example.com");
        actor.setPassword("secret");
        actor.setRole(UserRole.ADMIN);
        actor.setActive(true);
        CustomUserDetails principal = new CustomUserDetails(actor);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acceptedFromOpenAppendsOneTransitionAndLeavesTheSaleUnchanged() {
        OpenCase openCase = openDiscrepancy();

        DiscrepancyDetailResponseDTO response = service.changeStatus(
                openCase.merchantId(),
                openCase.runId(),
                openCase.discrepancyId(),
                new UpdateDiscrepancyStatusRequestDTO(DiscrepancyStatus.ACCEPTED, "reviewed", null));

        assertThat(response.status()).isEqualTo(DiscrepancyStatus.ACCEPTED);
        assertThat(openCase.discrepancy().getStatus()).isEqualTo(DiscrepancyStatus.ACCEPTED);
        assertThat(response.transitions()).singleElement().satisfies(transition -> {
            assertThat(transition.fromStatus()).isEqualTo(DiscrepancyStatus.OPEN);
            assertThat(transition.toStatus()).isEqualTo(DiscrepancyStatus.ACCEPTED);
            assertThat(transition.actorUserId()).isEqualTo(actorId);
            assertThat(transition.note()).isEqualTo("reviewed");
        });
        assertThat(response.adjustments()).isEmpty();
        assertThat(openCase.transaction().getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(openCase.transaction().getExpectedNetAmount()).isEqualByComparingTo("120.00");
        assertThat(openCase.item().getTransactionStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(openCase.item().getExpectedNetAmount()).isEqualByComparingTo("120.00");

        ArgumentCaptor<DiscrepancyTransitionEntity> transitions =
                ArgumentCaptor.forClass(DiscrepancyTransitionEntity.class);
        verify(transitionRepository).save(transitions.capture());
        assertThat(transitions.getAllValues()).hasSize(1);
        assertThat(transitions.getValue().getFromStatus()).isEqualTo(DiscrepancyStatus.OPEN);
        assertThat(transitions.getValue().getToStatus()).isEqualTo(DiscrepancyStatus.ACCEPTED);
        assertThat(transitions.getValue().getActor().getId()).isEqualTo(actorId);
        verify(adjustmentRepository, never()).save(any());
        verify(auditLogger).record(
                "DISCREPANCY_STATUS_CHANGED",
                "discrepancy",
                openCase.discrepancyId(),
                "OPEN -> ACCEPTED");
        verifyNoInteractions(userMerchantAccessRepository);
    }

    @Test
    void writtenOffFromOpenAppendsOneTransitionWithoutAnAdjustment() {
        OpenCase openCase = openDiscrepancy();

        DiscrepancyDetailResponseDTO response = service.changeStatus(
                openCase.merchantId(),
                openCase.runId(),
                openCase.discrepancyId(),
                new UpdateDiscrepancyStatusRequestDTO(DiscrepancyStatus.WRITTEN_OFF, null, null));

        assertThat(response.status()).isEqualTo(DiscrepancyStatus.WRITTEN_OFF);
        assertThat(openCase.discrepancy().getStatus()).isEqualTo(DiscrepancyStatus.WRITTEN_OFF);
        assertThat(response.transitions()).singleElement().satisfies(transition -> {
            assertThat(transition.fromStatus()).isEqualTo(DiscrepancyStatus.OPEN);
            assertThat(transition.toStatus()).isEqualTo(DiscrepancyStatus.WRITTEN_OFF);
            assertThat(transition.actorUserId()).isEqualTo(actorId);
            assertThat(transition.note()).isNull();
        });
        assertThat(response.adjustments()).isEmpty();
        assertThat(openCase.transaction().getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(openCase.transaction().getExpectedNetAmount()).isEqualByComparingTo("120.00");

        verify(transitionRepository).save(any(DiscrepancyTransitionEntity.class));
        verify(adjustmentRepository, never()).save(any());
        verify(auditLogger).record(
                "DISCREPANCY_STATUS_CHANGED",
                "discrepancy",
                openCase.discrepancyId(),
                "OPEN -> WRITTEN_OFF");
        verifyNoInteractions(userMerchantAccessRepository);
    }

    @Test
    void adjustedFromOpenStoresOneNonVoidedCorrectionAndLeavesExpectedNetAmount() {
        OpenCase openCase = openDiscrepancy();
        when(adjustmentRepository.save(any(DiscrepancyAdjustmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DiscrepancyDetailResponseDTO response = service.changeStatus(
                openCase.merchantId(),
                openCase.runId(),
                openCase.discrepancyId(),
                new UpdateDiscrepancyStatusRequestDTO(
                        DiscrepancyStatus.ADJUSTED, "correction", new BigDecimal("-1.50")));

        assertThat(response.status()).isEqualTo(DiscrepancyStatus.ADJUSTED);
        assertThat(openCase.discrepancy().getStatus()).isEqualTo(DiscrepancyStatus.ADJUSTED);
        assertThat(response.adjustments()).singleElement().satisfies(adjustment -> {
            assertThat(adjustment.amount()).isEqualByComparingTo("-1.50");
            assertThat(adjustment.voided()).isFalse();
        });
        assertThat(response.transitions()).hasSize(1);
        assertThat(openCase.transaction().getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(openCase.transaction().getExpectedNetAmount()).isEqualByComparingTo("120.00");
        assertThat(openCase.item().getExpectedNetAmount()).isEqualByComparingTo("120.00");

        ArgumentCaptor<DiscrepancyAdjustmentEntity> adjustments =
                ArgumentCaptor.forClass(DiscrepancyAdjustmentEntity.class);
        verify(adjustmentRepository).save(adjustments.capture());
        assertThat(adjustments.getAllValues()).hasSize(1);
        assertThat(adjustments.getValue().getAmount()).isEqualByComparingTo("-1.50");
        assertThat(adjustments.getValue().getVoidedAt()).isNull();
        assertThat(adjustments.getValue().getCreatedBy().getId()).isEqualTo(actorId);
        verify(transitionRepository).save(any(DiscrepancyTransitionEntity.class));
        verify(auditLogger).record(
                "DISCREPANCY_STATUS_CHANGED",
                "discrepancy",
                openCase.discrepancyId(),
                "OPEN -> ADJUSTED");
    }

    @Test
    void actorUserIdComesFromTheAuthenticatedPrincipalWithoutMerchantAccessLookup() {
        OpenCase openCase = openDiscrepancy();
        when(adjustmentRepository.save(any(DiscrepancyAdjustmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DiscrepancyDetailResponseDTO response = service.changeStatus(
                openCase.merchantId(),
                openCase.runId(),
                openCase.discrepancyId(),
                new UpdateDiscrepancyStatusRequestDTO(
                        DiscrepancyStatus.ADJUSTED, null, new BigDecimal("-1.50")));

        ArgumentCaptor<DiscrepancyTransitionEntity> transitions =
                ArgumentCaptor.forClass(DiscrepancyTransitionEntity.class);
        verify(transitionRepository).save(transitions.capture());
        assertThat(transitions.getValue().getActor().getId()).isEqualTo(actorId);
        assertThat(response.transitions()).singleElement()
                .extracting(DiscrepancyDetailResponseDTO.Transition::actorUserId)
                .isEqualTo(actorId);

        ArgumentCaptor<DiscrepancyAdjustmentEntity> adjustments =
                ArgumentCaptor.forClass(DiscrepancyAdjustmentEntity.class);
        verify(adjustmentRepository).save(adjustments.capture());
        assertThat(adjustments.getValue().getCreatedBy().getId()).isEqualTo(actorId);
        assertThat(adjustments.getValue().getAmount()).isEqualByComparingTo("-1.50");
        verify(userRepository).findById(actorId);
        verifyNoInteractions(userMerchantAccessRepository);
    }

    @Test
    void missingDiscrepancyIsNotSaved() {
        UUID discrepancyId = UUID.randomUUID();

        assertThatThrownBy(() -> service.changeStatus(
                UUID.randomUUID(),
                UUID.randomUUID(),
                discrepancyId,
                new UpdateDiscrepancyStatusRequestDTO(DiscrepancyStatus.ACCEPTED, null, null)))
                .isInstanceOf(DiscrepancyNotFoundException.class)
                .hasMessageContaining(discrepancyId.toString());

        verify(discrepancyRepository, never()).save(any());
        verify(transitionRepository, never()).save(any());
        verify(adjustmentRepository, never()).save(any());
        verifyNoInteractions(auditLogger);
        verifyNoInteractions(userMerchantAccessRepository);
    }

    @Test
    void reopenAcceptedReturnsToOpenWithoutCreatingAnAdjustment() {
        OpenCase openCase = openDiscrepancy();
        openCase.discrepancy().setStatus(DiscrepancyStatus.ACCEPTED);

        DiscrepancyDetailResponseDTO response = service.changeStatus(
                openCase.merchantId(),
                openCase.runId(),
                openCase.discrepancyId(),
                new UpdateDiscrepancyStatusRequestDTO(DiscrepancyStatus.OPEN, null, null));

        assertThat(response.status()).isEqualTo(DiscrepancyStatus.OPEN);
        assertThat(openCase.discrepancy().getStatus()).isEqualTo(DiscrepancyStatus.OPEN);
        assertThat(response.adjustments()).isEmpty();
        assertThat(response.transitions()).singleElement().satisfies(transition -> {
            assertThat(transition.fromStatus()).isEqualTo(DiscrepancyStatus.ACCEPTED);
            assertThat(transition.toStatus()).isEqualTo(DiscrepancyStatus.OPEN);
        });
        verify(adjustmentRepository, never()).save(any());
    }

    @Test
    void reopenAdjustedVoidsTheOnlyActiveAdjustment() {
        OpenCase openCase = openDiscrepancy();
        openCase.discrepancy().setStatus(DiscrepancyStatus.ADJUSTED);
        DiscrepancyAdjustmentEntity active = adjustment(new BigDecimal("-1.50"), null);
        openCase.discrepancy().setAdjustments(new ArrayList<>(List.of(active)));
        when(adjustmentRepository.save(any(DiscrepancyAdjustmentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DiscrepancyDetailResponseDTO response = service.changeStatus(
                openCase.merchantId(),
                openCase.runId(),
                openCase.discrepancyId(),
                new UpdateDiscrepancyStatusRequestDTO(DiscrepancyStatus.OPEN, null, null));

        assertThat(response.status()).isEqualTo(DiscrepancyStatus.OPEN);
        assertThat(active.getVoidedAt()).isNotNull();
        assertThat(response.adjustments()).singleElement().satisfies(adjustment -> {
            assertThat(adjustment.amount()).isEqualByComparingTo("-1.50");
            assertThat(adjustment.voided()).isTrue();
        });
        assertThat(openCase.discrepancy().getAdjustments()).hasSize(1);
        verify(adjustmentRepository).save(active);
    }

    @Test
    void terminalJumpAndRepeatedStatusDoNotSave() {
        UUID merchantId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID discrepancyId = UUID.randomUUID();
        ReconciliationDiscrepancyEntity discrepancy = completedDiscrepancy(runId, discrepancyId);
        discrepancy.setStatus(DiscrepancyStatus.ACCEPTED);
        when(discrepancyRepository.findByIdAndRunAndMerchant(discrepancyId, runId, merchantId))
                .thenReturn(Optional.of(discrepancy));

        UpdateDiscrepancyStatusRequestDTO jump =
                new UpdateDiscrepancyStatusRequestDTO(DiscrepancyStatus.ADJUSTED, null, new BigDecimal("1.00"));
        UpdateDiscrepancyStatusRequestDTO repeat =
                new UpdateDiscrepancyStatusRequestDTO(DiscrepancyStatus.ACCEPTED, null, null);

        assertThatThrownBy(() -> service.changeStatus(merchantId, runId, discrepancyId, jump))
                .isInstanceOf(DiscrepancyResolutionConflictException.class);
        assertThatThrownBy(() -> service.changeStatus(merchantId, runId, discrepancyId, repeat))
                .isInstanceOf(DiscrepancyResolutionConflictException.class);

        assertThat(discrepancy.getStatus()).isEqualTo(DiscrepancyStatus.ACCEPTED);
        verify(discrepancyRepository, never()).save(any());
        verify(transitionRepository, never()).save(any());
        verify(adjustmentRepository, never()).save(any());
    }

    @Test
    void getReturnsAdjustmentsAndTransitionsInCreatedAtOrder() {
        UUID merchantId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID discrepancyId = UUID.randomUUID();
        ReconciliationDiscrepancyEntity discrepancy = new ReconciliationDiscrepancyEntity();
        discrepancy.setId(discrepancyId);
        discrepancy.setType(DiscrepancyType.INCORRECT_AMOUNT);
        discrepancy.setExpectedValue("10.00");
        discrepancy.setActualValue("8.00");
        discrepancy.setStatus(DiscrepancyStatus.ADJUSTED);

        DiscrepancyAdjustmentEntity voided = adjustment(new BigDecimal("2.00"), Instant.parse("2026-10-01T00:00:00Z"));
        DiscrepancyAdjustmentEntity active = adjustment(new BigDecimal("-1.50"), null);
        discrepancy.setAdjustments(new ArrayList<>(List.of(voided, active)));

        DiscrepancyTransitionEntity later = transition(
                DiscrepancyStatus.ACCEPTED, DiscrepancyStatus.OPEN, Instant.parse("2026-10-02T00:00:00Z"));
        DiscrepancyTransitionEntity earlier = transition(
                DiscrepancyStatus.OPEN, DiscrepancyStatus.ACCEPTED, Instant.parse("2026-10-01T00:00:00Z"));
        discrepancy.setTransitions(new ArrayList<>(List.of(later, earlier)));

        when(discrepancyRepository.findByIdAndRunAndMerchant(discrepancyId, runId, merchantId))
                .thenReturn(Optional.of(discrepancy));

        DiscrepancyDetailResponseDTO response = service.get(merchantId, runId, discrepancyId);

        assertThat(response.status()).isEqualTo(DiscrepancyStatus.ADJUSTED);
        assertThat(response.adjustments()).extracting(DiscrepancyDetailResponseDTO.Adjustment::amount)
                .containsExactly(new BigDecimal("2.00"), new BigDecimal("-1.50"));
        assertThat(response.adjustments()).extracting(DiscrepancyDetailResponseDTO.Adjustment::voided)
                .containsExactly(true, false);
        assertThat(response.transitions()).extracting(DiscrepancyDetailResponseDTO.Transition::createdAt)
                .containsExactly(Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-02T00:00:00Z"));
        assertThat(response.transitions()).extracting(DiscrepancyDetailResponseDTO.Transition::actorUserId)
                .containsExactly(actorId, actorId);
    }

    private OpenCase openDiscrepancy() {
        UUID merchantId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID discrepancyId = UUID.randomUUID();
        ReconciliationDiscrepancyEntity discrepancy = completedDiscrepancy(runId, discrepancyId);

        when(discrepancyRepository.findByIdAndRunAndMerchant(discrepancyId, runId, merchantId))
                .thenReturn(Optional.of(discrepancy));
        when(discrepancyRepository.save(any(ReconciliationDiscrepancyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transitionRepository.save(any(DiscrepancyTransitionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));

        return new OpenCase(
                merchantId,
                runId,
                discrepancyId,
                discrepancy,
                discrepancy.getReconciliationItem(),
                discrepancy.getReconciliationItem().getInternalTransaction());
    }

    private ReconciliationDiscrepancyEntity completedDiscrepancy(UUID runId, UUID discrepancyId) {
        ReconciliationRunEntity run = new ReconciliationRunEntity();
        run.setId(runId);
        run.setStatus(ReconciliationRunStatus.COMPLETED);
        run.setSupersededAt(null);

        InternalTransactionEntity transaction = new InternalTransactionEntity();
        transaction.setId(UUID.randomUUID());
        transaction.setStatus(TransactionStatus.APPROVED);
        transaction.setExpectedNetAmount(new BigDecimal("120.00"));

        ReconciliationItemEntity item = new ReconciliationItemEntity();
        item.setId(UUID.randomUUID());
        item.setReconciliationRun(run);
        item.setInternalTransaction(transaction);
        item.setTransactionStatus(TransactionStatus.APPROVED);
        item.setExpectedNetAmount(new BigDecimal("120.00"));

        ReconciliationDiscrepancyEntity discrepancy = new ReconciliationDiscrepancyEntity();
        discrepancy.setId(discrepancyId);
        discrepancy.setReconciliationItem(item);
        discrepancy.setType(DiscrepancyType.FEE_DIVERGENCE);
        discrepancy.setExpectedValue("100.00");
        discrepancy.setActualValue("98.50");
        discrepancy.setStatus(DiscrepancyStatus.OPEN);
        return discrepancy;
    }

    private DiscrepancyAdjustmentEntity adjustment(BigDecimal amount, Instant voidedAt) {
        DiscrepancyAdjustmentEntity adjustment = new DiscrepancyAdjustmentEntity();
        adjustment.setAmount(amount);
        adjustment.setCreatedBy(actor);
        adjustment.setCreatedAt(Instant.parse("2026-10-01T00:00:00Z"));
        adjustment.setVoidedAt(voidedAt);
        return adjustment;
    }

    private DiscrepancyTransitionEntity transition(
            DiscrepancyStatus fromStatus,
            DiscrepancyStatus toStatus,
            Instant createdAt) {
        DiscrepancyTransitionEntity transition = new DiscrepancyTransitionEntity();
        transition.setActor(actor);
        transition.setFromStatus(fromStatus);
        transition.setToStatus(toStatus);
        transition.setCreatedAt(createdAt);
        return transition;
    }

    private record OpenCase(
            UUID merchantId,
            UUID runId,
            UUID discrepancyId,
            ReconciliationDiscrepancyEntity discrepancy,
            ReconciliationItemEntity item,
            InternalTransactionEntity transaction
    ) {
    }
}
