package br.com.hanrry.reconpay.auth.email;

public final class EmailVerificationPages {

    public static final String SUCCESS = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head><meta charset="UTF-8"/><title>ReconPay — E-mail confirmado</title></head>
            <body style="font-family: system-ui, sans-serif; max-width: 32rem; margin: 2rem auto;">
              <h1>E-mail confirmado</h1>
              <p>Sua conta está ativa. Você já pode fazer login no ReconPay.</p>
            </body>
            </html>
            """;

    public static final String INVALID = """
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head><meta charset="UTF-8"/><title>ReconPay — Link inválido</title></head>
            <body style="font-family: system-ui, sans-serif; max-width: 32rem; margin: 2rem auto;">
              <h1>Não foi possível confirmar</h1>
              <p>Este link expirou, já foi usado ou é inválido. Cadastre-se novamente ou peça um novo e-mail.</p>
            </body>
            </html>
            """;

    private EmailVerificationPages() {
    }
}
