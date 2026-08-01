package br.com.hanrry.reconpay.exception;

public class DuplicateExternalReferenceException extends RuntimeException {

    public DuplicateExternalReferenceException(String message) {
        super(message);
    }
}
