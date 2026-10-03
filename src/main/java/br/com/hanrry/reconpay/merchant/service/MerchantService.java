package br.com.hanrry.reconpay.merchant.service;

import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.auth.repository.IUserMerchantAccessRepository;
import br.com.hanrry.reconpay.auth.service.UserMerchantAccessService;
import br.com.hanrry.reconpay.exception.MerchantAlreadyExistsException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.merchant.dto.MerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.dto.MerchantResponseDTO;
import br.com.hanrry.reconpay.merchant.dto.UpdateMerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.mapper.IMerchantMapper;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.security.AuthenticatedUserAccessor;
import br.com.hanrry.reconpay.security.CustomUserDetails;
import br.com.hanrry.reconpay.security.MerchantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final IMerchantMapper merchantMapper;
    private final IMerchantRepository merchantRepository;
    private final IUserMerchantAccessRepository userMerchantAccessRepository;
    private final UserMerchantAccessService userMerchantAccessService;
    private final MerchantAccessGuard merchantAccessGuard;
    private final AuthenticatedUserAccessor authenticatedUserAccessor;
    private final AuditLogger auditLogger;

    @Transactional
    public MerchantResponseDTO create(MerchantRequestDTO request) {
        if (merchantRepository.existsByDocument(request.document())) {
            throw new MerchantAlreadyExistsException(
                    "Comerciante já cadastrado com documento: " + request.document());
        }

        MerchantEntity entity = merchantMapper.toEntity(request);
        MerchantEntity savedMerchant = merchantRepository.save(entity);

        CustomUserDetails creator = authenticatedUserAccessor.requirePrincipal();
        userMerchantAccessService.grantIfAbsent(creator.getId(), savedMerchant.getId());

        auditLogger.record("MERCHANT_CREATED", "merchant", savedMerchant.getId());
        return merchantMapper.toDTO(savedMerchant);
    }

    public Page<MerchantResponseDTO> findAllActive(Pageable pageable) {
        CustomUserDetails principal = authenticatedUserAccessor.requirePrincipal();

        if (principal.getRole() == UserRole.ADMIN) {
            return merchantRepository.findAllByActiveTrue(pageable)
                    .map(merchantMapper::toDTO);
        }

        List<UUID> merchantIds = userMerchantAccessRepository.findAllByUserId(principal.getId()).stream()
                .map(access -> access.getMerchantId())
                .toList();

        if (merchantIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return merchantRepository.findAllByActiveTrueAndIdIn(merchantIds, pageable)
                .map(merchantMapper::toDTO);
    }

    public MerchantResponseDTO findById(UUID id) {
        merchantAccessGuard.requireAccess(id);
        MerchantEntity entity = merchantRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new MerchantNotFoundException("Comerciante não encontrado com id: " + id));
        return merchantMapper.toDTO(entity);
    }

    @Transactional
    public MerchantResponseDTO update(UUID id, UpdateMerchantRequestDTO request) {
        merchantAccessGuard.requireAccess(id);
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new MerchantNotFoundException("Comerciante não encontrado com id: " + id));

        if (request.name() != null && !request.name().isBlank()) {
            merchant.setName(request.name());
        }

        MerchantEntity savedMerchant = merchantRepository.save(merchant);
        auditLogger.record("MERCHANT_UPDATED", "merchant", id);
        return merchantMapper.toDTO(savedMerchant);
    }

    @Transactional
    public void deleteById(UUID id) {
        merchantAccessGuard.requireAccess(id);
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new MerchantNotFoundException("Comerciante não encontrado com id: " + id));
        merchant.setActive(false);
        merchantRepository.save(merchant);
        auditLogger.record("MERCHANT_DEACTIVATED", "merchant", id);
    }
}
