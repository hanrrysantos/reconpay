package br.com.hanrry.reconpay.exception;

public class InvalidTransactionStatusTransitionException extends RuntimeException {

    public InvalidTransactionStatusTransitionException(String message) {
        super(message);
    }
}
