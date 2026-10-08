package br.com.hanrry.reconpay.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserAccessor {

    public CustomUserDetails requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails principal) {
            return principal;
        }

        throw new AccessDeniedException("Usuário não autenticado");
    }
}
