package br.com.hanrry.reconpay.exception;

public class ExternalSettlementNotFoundException extends RuntimeException {

    public ExternalSettlementNotFoundException(String message) {
        super(message);
    }
}
