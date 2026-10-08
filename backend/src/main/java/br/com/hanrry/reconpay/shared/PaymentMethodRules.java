package br.com.hanrry.reconpay.shared;

import br.com.hanrry.reconpay.shared.enums.PaymentMethod;

import java.util.EnumSet;
import java.util.Set;

public final class PaymentMethodRules {

    public static final Set<PaymentMethod> SINGLE_INSTALLMENT_METHODS = EnumSet.of(
            PaymentMethod.PIX,
            PaymentMethod.BOLETO,
            PaymentMethod.DEBIT_CARD
    );

    private PaymentMethodRules() {
    }

    public static boolean allowsInstallments(PaymentMethod paymentMethod, int installments) {
        return !SINGLE_INSTALLMENT_METHODS.contains(paymentMethod) || installments <= 1;
    }
}
