package br.com.hanrry.reconpay.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthenticatedUserAccessor {

    public CustomUserDetails requirePrincipal() {
        return currentPrincipal().orElseThrow(() -> new AccessDeniedException("Usuário não autenticado"));
    }

    /** Empty when nobody is authenticated, as in the anonymous demo creation. */
    public Optional<CustomUserDetails> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails principal) {
            return Optional.of(principal);
        }

        return Optional.empty();
    }
}
