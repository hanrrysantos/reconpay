package br.com.hanrry.reconpay.exception;

public class MerchantAlreadyExistsException extends RuntimeException {
    public MerchantAlreadyExistsException(String message) {
        super(message);
    }
}
