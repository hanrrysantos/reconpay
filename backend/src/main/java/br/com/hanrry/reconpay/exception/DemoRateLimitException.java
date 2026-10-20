package br.com.hanrry.reconpay.exception;

public class DemoRateLimitException extends RuntimeException {

    public DemoRateLimitException() {
        super("Limite de demonstrações atingido. Tente novamente em até 1 hora.");
    }
}
