package br.com.hanrry.reconpay.exception;

public class DemoUnavailableException extends RuntimeException {

    public DemoUnavailableException() {
        super("Demonstrações esgotadas no momento. Tente novamente mais tarde.");
    }
}
