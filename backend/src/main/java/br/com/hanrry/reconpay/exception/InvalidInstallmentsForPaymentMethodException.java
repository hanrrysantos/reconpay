package br.com.hanrry.reconpay.exception;

public class InvalidInstallmentsForPaymentMethodException extends RuntimeException {

    public InvalidInstallmentsForPaymentMethodException(String message) {
        super(message);
    }
}
