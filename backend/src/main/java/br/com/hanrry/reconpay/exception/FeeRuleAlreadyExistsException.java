package br.com.hanrry.reconpay.exception;

public class FeeRuleAlreadyExistsException extends RuntimeException {
    public FeeRuleAlreadyExistsException(String message) {
        super(message);
    }
}
