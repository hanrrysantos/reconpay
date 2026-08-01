package br.com.hanrry.reconpay.exception;

public class MissingActiveFeeRuleException extends RuntimeException {

    public MissingActiveFeeRuleException(String message) {
        super(message);
    }
}
