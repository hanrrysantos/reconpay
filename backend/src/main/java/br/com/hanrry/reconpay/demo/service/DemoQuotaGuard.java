package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.repository.IUserMerchantAccessRepository;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.demo.config.DemoProperties;
import br.com.hanrry.reconpay.exception.DemoQuotaExceededException;
import br.com.hanrry.reconpay.security.AuthenticatedUserAccessor;
import br.com.hanrry.reconpay.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/*
 * Write quotas of a demo account. Callers invoke it first thing inside their own transaction:
 * the user row lock serializes requests of the same demo, and a failed import rolls back the
 * counter. Anonymous calls (the demo scenario) and regular accounts pass untouched.
 */
@Service
@RequiredArgsConstructor
public class DemoQuotaGuard {

    private final AuthenticatedUserAccessor authenticatedUserAccessor;
    private final IUserRepository userRepository;
    private final IUserMerchantAccessRepository userMerchantAccessRepository;
    private final DemoProperties properties;

    @Transactional
    public void checkMerchantCreation() {
        lockedDemoUser().ifPresent(user -> {
            if (userMerchantAccessRepository.countByUserId(user.getId()) >= properties.maxMerchants()) {
                throw DemoQuotaExceededException.merchants(properties.maxMerchants());
            }
        });
    }

    @Transactional
    public void consumeImport() {
        lockedDemoUser().ifPresent(user -> {
            if (user.getDemoImportCount() >= properties.maxImports()) {
                throw DemoQuotaExceededException.imports(properties.maxImports());
            }
            user.setDemoImportCount(user.getDemoImportCount() + 1);
        });
    }

    private Optional<UserEntity> lockedDemoUser() {
        return authenticatedUserAccessor.currentPrincipal()
                .filter(CustomUserDetails::isDemo)
                .flatMap(principal -> userRepository.findByIdForUpdate(principal.getId()));
    }
}
