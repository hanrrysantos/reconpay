package br.com.hanrry.reconpay.exception;

public class InvalidTransactionImportException extends RuntimeException {

    public InvalidTransactionImportException(String message) {
        super(message);
    }
}
