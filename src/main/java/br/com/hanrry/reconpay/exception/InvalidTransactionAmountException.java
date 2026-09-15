package br.com.hanrry.reconpay.exception;

public class InvalidTransactionAmountException extends IllegalArgumentException {
    public InvalidTransactionAmountException(String message) {
        super(message);
    }
}
