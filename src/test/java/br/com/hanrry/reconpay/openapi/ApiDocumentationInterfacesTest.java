package br.com.hanrry.reconpay.openapi;

import br.com.hanrry.reconpay.auth.controller.AuthController;
import br.com.hanrry.reconpay.auth.controller.MeController;
import br.com.hanrry.reconpay.auth.controller.UserController;
import br.com.hanrry.reconpay.bankstatement.controller.BankStatementController;
import br.com.hanrry.reconpay.externalsettlement.controller.ExternalSettlementController;
import br.com.hanrry.reconpay.feerule.controller.FeeRuleController;
import br.com.hanrry.reconpay.merchant.controller.MerchantController;
import br.com.hanrry.reconpay.reconciliation.controller.ReconciliationController;
import br.com.hanrry.reconpay.transaction.controller.TransactionController;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiDocumentationInterfacesTest {

    private static final List<Class<?>> CONTROLLERS = List.of(
            AuthController.class,
            MeController.class,
            UserController.class,
            MerchantController.class,
            FeeRuleController.class,
            TransactionController.class,
            ExternalSettlementController.class,
            BankStatementController.class,
            ReconciliationController.class
    );

    @Test
    void nineControllersImplementAnApiInterfaceAndDoNotDeclareOperation() {
        assertThat(CONTROLLERS).hasSize(9);
        for (Class<?> controller : CONTROLLERS) {
            long apiInterfaces = Arrays.stream(controller.getInterfaces())
                    .filter(type -> type.getSimpleName().endsWith("Api"))
                    .count();
            assertThat(apiInterfaces)
                    .as("%s implements an interface whose name ends with Api", controller.getSimpleName())
                    .isEqualTo(1);

            for (Method method : controller.getDeclaredMethods()) {
                if (method.isSynthetic()) {
                    continue;
                }
                assertThat(method.isAnnotationPresent(Operation.class))
                        .as("%s#%s declares @Operation", controller.getSimpleName(), method.getName())
                        .isFalse();
            }
        }
    }
}
