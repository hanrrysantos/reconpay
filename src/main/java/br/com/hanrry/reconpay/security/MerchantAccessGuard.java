package br.com.hanrry.reconpay.security;

import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.auth.repository.IUserMerchantAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Decides whether the caller may act on a given merchant.
 *
 * <p>Role checks in the filter chain only answer "may this role read
 * transactions", never "whose transactions". Access is denied unless the caller
 * is an administrator or holds an explicit grant, so a merchant added later is
 * invisible until someone deliberately shares it.
 */
@Component
@RequiredArgsConstructor
public class MerchantAccessGuard {

    private final IUserMerchantAccessRepository userMerchantAccessRepository;

    public void requireAccess(UUID merchantId) {
        if (merchantId == null || hasAccess(merchantId)) {
            return;
        }

        throw new AccessDeniedException("Sem acesso ao comerciante informado");
    }

    private boolean hasAccess(UUID merchantId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            return false;
        }

        if (principal.getRole() == UserRole.ADMIN) {
            return true;
        }

        return userMerchantAccessRepository.existsByUserIdAndMerchantId(principal.getId(), merchantId);
    }
}
