package br.com.hanrry.reconpay.exception;

public class InvalidReconciliationWindowException extends RuntimeException {

    public InvalidReconciliationWindowException(String message) {
        super(message);
    }
}
