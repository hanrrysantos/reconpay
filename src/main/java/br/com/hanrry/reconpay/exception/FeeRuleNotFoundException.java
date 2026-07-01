package br.com.hanrry.reconpay.exception;

public class FeeRuleNotFoundException extends RuntimeException {
    public FeeRuleNotFoundException(String message) {
        super(message);
    }
}
