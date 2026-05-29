package br.com.hanrry.reconpay.merchant.exception;

public class MerchantAlreadyExistsException extends RuntimeException {
    public MerchantAlreadyExistsException(String message) {
        super(message);
    }
}
