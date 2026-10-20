package br.com.hanrry.reconpay.exception;

public class DemoQuotaExceededException extends RuntimeException {

    private DemoQuotaExceededException(String message) {
        super(message);
    }

    public static DemoQuotaExceededException merchants(int max) {
        return new DemoQuotaExceededException("A demonstração permite no máximo " + max + " empresas.");
    }

    public static DemoQuotaExceededException imports(int max) {
        return new DemoQuotaExceededException("A demonstração permite no máximo " + max + " importações.");
    }
}
