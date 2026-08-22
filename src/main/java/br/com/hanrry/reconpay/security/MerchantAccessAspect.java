package br.com.hanrry.reconpay.security;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Enforces {@link MerchantAccessGuard} on every controller handler that takes a
 * merchant id, so a new merchant-scoped endpoint is covered by default instead
 * of only when its author remembers to add the check.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class MerchantAccessAspect {

    private static final String MERCHANT_ID_PARAMETER = "merchantId";

    private final MerchantAccessGuard merchantAccessGuard;

    @Before("within(br.com.hanrry.reconpay..controller..*)")
    public void checkMerchantAccess(JoinPoint joinPoint) {
        if (!(joinPoint.getSignature() instanceof MethodSignature signature)) {
            return;
        }

        String[] parameterNames = signature.getParameterNames();
        if (parameterNames == null) {
            return;
        }

        Object[] arguments = joinPoint.getArgs();

        for (int index = 0; index < parameterNames.length; index++) {
            if (MERCHANT_ID_PARAMETER.equals(parameterNames[index])
                    && arguments[index] instanceof UUID merchantId) {
                merchantAccessGuard.requireAccess(merchantId);
                return;
            }
        }
    }
}
