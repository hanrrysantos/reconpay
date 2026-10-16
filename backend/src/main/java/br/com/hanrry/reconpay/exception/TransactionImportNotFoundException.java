package br.com.hanrry.reconpay.exception;

public class TransactionImportNotFoundException extends RuntimeException {

    public TransactionImportNotFoundException(String message) {
        super(message);
    }
}
