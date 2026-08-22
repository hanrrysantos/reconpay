package br.com.hanrry.reconpay.auth.service;

import br.com.hanrry.reconpay.auth.dto.MerchantAccessRequestDTO;
import br.com.hanrry.reconpay.auth.dto.MerchantAccessResponseDTO;
import br.com.hanrry.reconpay.auth.entity.UserMerchantAccessEntity;
import br.com.hanrry.reconpay.auth.repository.IUserMerchantAccessRepository;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.UserNotFoundException;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserMerchantAccessService {

    private final IUserMerchantAccessRepository accessRepository;
    private final IUserRepository userRepository;
    private final IMerchantRepository merchantRepository;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public MerchantAccessResponseDTO findByUser(UUID userId) {
        requireUser(userId);
        return new MerchantAccessResponseDTO(userId, merchantIdsOf(userId));
    }

    /**
     * Replaces the whole grant set. Granting and revoking in one call keeps the
     * caller from having to diff the current state, and makes the audit entry a
     * single record of what access the user ended up with.
     */
    @Transactional
    public MerchantAccessResponseDTO replace(UUID userId, MerchantAccessRequestDTO request) {
        requireUser(userId);

        List<UUID> merchantIds = request.merchantIds().stream().distinct().toList();
        merchantIds.forEach(this::requireMerchant);

        accessRepository.deleteAllByUserId(userId);
        accessRepository.flush();
        accessRepository.saveAll(
                merchantIds.stream()
                        .map(merchantId -> new UserMerchantAccessEntity(userId, merchantId))
                        .toList()
        );

        auditLogger.record("USER_MERCHANT_ACCESS_REPLACED", "user", userId, "merchantIds=" + merchantIds);

        return new MerchantAccessResponseDTO(userId, merchantIds);
    }

    private List<UUID> merchantIdsOf(UUID userId) {
        return accessRepository.findAllByUserId(userId).stream()
                .map(UserMerchantAccessEntity::getMerchantId)
                .toList();
    }

    private void requireUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("Usuário não encontrado com id: " + userId);
        }
    }

    private void requireMerchant(UUID merchantId) {
        if (merchantRepository.findByIdAndActiveTrue(merchantId).isEmpty()) {
            throw new MerchantNotFoundException("Comerciante não encontrado com id: " + merchantId);
        }
    }
}
