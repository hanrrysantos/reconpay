package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.exception.DiscrepancyNotFoundException;
import br.com.hanrry.reconpay.exception.DiscrepancyResolutionConflictException;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.reconciliation.dto.DiscrepancyDetailResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.UpdateDiscrepancyStatusRequestDTO;
import br.com.hanrry.reconpay.reconciliation.entity.DiscrepancyAdjustmentEntity;
import br.com.hanrry.reconpay.reconciliation.entity.DiscrepancyTransitionEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IDiscrepancyAdjustmentRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IDiscrepancyTransitionRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationDiscrepancyRepository;
import br.com.hanrry.reconpay.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscrepancyResolutionService {

    private static final Set<DiscrepancyStatus> CLOSABLE_FROM_OPEN = EnumSet.of(
            DiscrepancyStatus.ACCEPTED,
            DiscrepancyStatus.WRITTEN_OFF,
            DiscrepancyStatus.ADJUSTED);

    private final IReconciliationDiscrepancyRepository discrepancyRepository;
    private final IDiscrepancyTransitionRepository transitionRepository;
    private final IDiscrepancyAdjustmentRepository adjustmentRepository;
    private final IUserRepository userRepository;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public DiscrepancyDetailResponseDTO get(UUID merchantId, UUID runId, UUID discrepancyId) {
        return toDetail(findDiscrepancy(merchantId, runId, discrepancyId));
    }

    @Transactional
    public DiscrepancyDetailResponseDTO changeStatus(
            UUID merchantId,
            UUID runId,
            UUID discrepancyId,
            UpdateDiscrepancyStatusRequestDTO request) {
        ReconciliationDiscrepancyEntity discrepancy = findDiscrepancy(merchantId, runId, discrepancyId);
        ensureRunAcceptsResolution(discrepancy);

        DiscrepancyStatus current = discrepancy.getStatus();
        DiscrepancyStatus target = request.status();
        ensureCanClose(current, target);

        UserEntity actor = requireActor();
        Instant now = Instant.now();
        String note = blankToNull(request.note());

        DiscrepancyTransitionEntity transition = new DiscrepancyTransitionEntity();
        transition.setDiscrepancy(discrepancy);
        transition.setActor(actor);
        transition.setFromStatus(current);
        transition.setToStatus(target);
        transition.setNote(note);
        transition.setCreatedAt(now);
        DiscrepancyTransitionEntity savedTransition = transitionRepository.save(transition);

        DiscrepancyAdjustmentEntity savedAdjustment = null;
        if (target == DiscrepancyStatus.ADJUSTED) {
            DiscrepancyAdjustmentEntity adjustment = new DiscrepancyAdjustmentEntity();
            adjustment.setDiscrepancy(discrepancy);
            adjustment.setAmount(request.correctionAmount());
            adjustment.setCreatedBy(actor);
            adjustment.setCreatedAt(now);
            adjustment.setVoidedAt(null);
            savedAdjustment = adjustmentRepository.save(adjustment);
        }

        discrepancy.setStatus(target);
        discrepancy.getTransitions().add(savedTransition);
        if (savedAdjustment != null) {
            discrepancy.getAdjustments().add(savedAdjustment);
        }
        discrepancyRepository.save(discrepancy);

        // AuditLogger emits only after commit when a transaction is active.
        auditLogger.record(
                "DISCREPANCY_STATUS_CHANGED",
                "discrepancy",
                discrepancyId,
                current + " -> " + target);
        return toDetail(discrepancy);
    }

    private ReconciliationDiscrepancyEntity findDiscrepancy(
            UUID merchantId,
            UUID runId,
            UUID discrepancyId) {
        return discrepancyRepository.findByIdAndRunAndMerchant(discrepancyId, runId, merchantId)
                .orElseThrow(() -> new DiscrepancyNotFoundException(
                        "Divergência não encontrada com id: " + discrepancyId));
    }

    private void ensureRunAcceptsResolution(ReconciliationDiscrepancyEntity discrepancy) {
        ReconciliationRunEntity run = discrepancy.getReconciliationItem().getReconciliationRun();
        if (run.getStatus() != ReconciliationRunStatus.COMPLETED || run.getSupersededAt() != null) {
            throw new DiscrepancyResolutionConflictException(
                    "Run de conciliação não aceita alteração de divergência");
        }
    }

    private void ensureCanClose(DiscrepancyStatus current, DiscrepancyStatus target) {
        if (current == DiscrepancyStatus.OPEN && CLOSABLE_FROM_OPEN.contains(target)) {
            return;
        }
        throw new DiscrepancyResolutionConflictException(
                "Transição de status inválida: " + current + " -> " + target);
    }

    private UserEntity requireActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new AccessDeniedException("Usuário não autenticado");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Usuário autenticado não encontrado com id: " + principal.getId()));
    }

    private DiscrepancyDetailResponseDTO toDetail(ReconciliationDiscrepancyEntity discrepancy) {
        List<DiscrepancyDetailResponseDTO.Adjustment> adjustments = adjustmentsOf(discrepancy).stream()
                .map(adjustment -> new DiscrepancyDetailResponseDTO.Adjustment(
                        adjustment.getAmount(),
                        adjustment.getVoidedAt() != null))
                .toList();
        List<DiscrepancyDetailResponseDTO.Transition> transitions = transitionsOf(discrepancy).stream()
                .sorted(Comparator.comparing(DiscrepancyTransitionEntity::getCreatedAt))
                .map(transition -> new DiscrepancyDetailResponseDTO.Transition(
                        transition.getActor().getId(),
                        transition.getFromStatus(),
                        transition.getToStatus(),
                        transition.getNote(),
                        transition.getCreatedAt()))
                .toList();
        return new DiscrepancyDetailResponseDTO(
                discrepancy.getId(),
                discrepancy.getType(),
                discrepancy.getExpectedValue(),
                discrepancy.getActualValue(),
                discrepancy.getStatus(),
                adjustments,
                transitions);
    }

    private List<DiscrepancyAdjustmentEntity> adjustmentsOf(ReconciliationDiscrepancyEntity discrepancy) {
        return discrepancy.getAdjustments() == null ? List.of() : discrepancy.getAdjustments();
    }

    private List<DiscrepancyTransitionEntity> transitionsOf(ReconciliationDiscrepancyEntity discrepancy) {
        return discrepancy.getTransitions() == null ? List.of() : discrepancy.getTransitions();
    }

    private static String blankToNull(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        return note;
    }
}
