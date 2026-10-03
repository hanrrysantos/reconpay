package br.com.hanrry.reconpay.auth.service;

import br.com.hanrry.reconpay.auth.dto.AccessibleMerchantResponseDTO;
import br.com.hanrry.reconpay.auth.dto.MeResponseDTO;
import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.auth.repository.IUserMerchantAccessRepository;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.security.AuthenticatedUserAccessor;
import br.com.hanrry.reconpay.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeService {

    private final AuthenticatedUserAccessor authenticatedUserAccessor;
    private final IUserMerchantAccessRepository userMerchantAccessRepository;
    private final IMerchantRepository merchantRepository;

    @Transactional(readOnly = true)
    public MeResponseDTO currentUser() {
        CustomUserDetails principal = authenticatedUserAccessor.requirePrincipal();
        return new MeResponseDTO(
                principal.getId(),
                principal.getName(),
                principal.getUsername(),
                principal.getRole(),
                principal.isEnabled()
        );
    }

    @Transactional(readOnly = true)
    public Page<AccessibleMerchantResponseDTO> accessibleMerchants(Pageable pageable) {
        CustomUserDetails principal = authenticatedUserAccessor.requirePrincipal();

        if (principal.getRole() == UserRole.ADMIN) {
            return merchantRepository.findAllByActiveTrue(pageable)
                    .map(this::toAccessibleMerchant);
        }

        List<UUID> merchantIds = userMerchantAccessRepository.findAllByUserId(principal.getId()).stream()
                .map(access -> access.getMerchantId())
                .toList();

        if (merchantIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return merchantRepository.findAllByActiveTrueAndIdIn(merchantIds, pageable)
                .map(this::toAccessibleMerchant);
    }

    private AccessibleMerchantResponseDTO toAccessibleMerchant(MerchantEntity merchant) {
        return new AccessibleMerchantResponseDTO(
                merchant.getId(),
                merchant.getName(),
                merchant.getDocument()
        );
    }
}
