package br.com.hanrry.reconpay.exception;

public class InvalidEmailVerificationTokenException extends RuntimeException {

    public InvalidEmailVerificationTokenException(String message) {
        super(message);
    }
}
