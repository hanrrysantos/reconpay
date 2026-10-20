package br.com.hanrry.reconpay.auth.email;

import br.com.hanrry.reconpay.auth.config.EmailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VerificationEmailComposer {

    private static final String SUBJECT = "Confirme seu e-mail — ReconPay";
    private static final String FONT = "Inter, Helvetica, Arial, sans-serif";

    private final EmailVerificationLinkFactory verificationLinkFactory;
    private final EmailProperties emailProperties;

    public VerificationEmail compose(String toEmail, String recipientName, String rawToken) {
        String link = verificationLinkFactory.buildConfirmUrl(rawToken);
        String expiry = "O link vale por " + validity() + " e só pode ser usado uma vez.";
        String brandMark = verificationLinkFactory.buildBrandMarkUrl();
        return new VerificationEmail(SUBJECT, html(toEmail, recipientName, link, expiry, brandMark), text(toEmail, recipientName, link, expiry));
    }

    private String validity() {
        long hours = emailProperties.verificationTokenHours();
        return hours == 1 ? "1 hora" : hours + " horas";
    }

    private static String html(String toEmail, String recipientName, String link, String expiry, String brandMark) {
        String href = escapeHtml(link);
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width, initial-scale=1"/><title>%1$s</title></head>
                <body style="margin:0;padding:0;background:#F4F5F1;font-family:%2$s;color:#0B0D0A;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#F4F5F1;">
                <tr><td align="center" style="padding:48px 16px;">
                <table role="presentation" width="560" cellpadding="0" cellspacing="0" border="0" style="width:100%%;max-width:560px;background:#FFFFFF;border-radius:24px;overflow:hidden;">
                <tr><td style="background:#0B0D0A;padding:28px 40px;">
                <img src="%7$s" width="32" height="32" alt="" style="display:inline-block;width:32px;height:32px;border:0;vertical-align:middle;"/>
                <span style="margin-left:10px;font-family:Manrope,%2$s;color:#FFFFFF;font-size:19px;font-weight:800;letter-spacing:-0.6px;vertical-align:middle;">Recon<span style="color:#C6F135;">Pay</span></span>
                </td></tr>
                <tr><td style="padding:40px 40px 36px 40px;">
                <p style="margin:0 0 14px 0;font-size:15px;font-weight:600;color:#6B6F66;">Olá, %3$s!</p>
                <h1 style="margin:0 0 14px 0;font-size:34px;line-height:1.05;font-weight:800;letter-spacing:-1.3px;color:#0B0D0A;">Confirme seu <span style="white-space:nowrap;">e-mail</span></h1>
                <p style="margin:0 0 28px 0;font-size:15px;line-height:1.6;color:#0B0D0A;">Falta um passo para ativar sua conta no ReconPay. Confirme que %4$s é seu e entre para cadastrar a primeira empresa.</p>
                <a href="%5$s" style="display:inline-block;padding:15px 28px;border-radius:999px;background:#C6F135;color:#0B0D0A;font-size:15px;font-weight:700;text-decoration:none;">Confirmar e-mail</a>
                <p style="margin:28px 0 24px 0;font-size:13px;line-height:1.5;color:#6B6F66;">%6$s</p>
                <hr style="border:0;border-top:1px solid #E6E8E1;margin:0 0 24px 0;"/>
                <p style="margin:0 0 8px 0;font-size:12px;line-height:1.5;color:#6B6F66;">Se o botão não funcionar, copie e cole este endereço no navegador:</p>
                <p style="margin:0;padding:10px 12px;border-radius:10px;background:#F8F9F5;font-family:'IBM Plex Mono',Menlo,Consolas,monospace;font-size:11px;line-height:1.5;color:#0B0D0A;word-break:break-all;">%5$s</p>
                </td></tr>
                </table>
                <p style="max-width:480px;margin:24px auto 6px auto;font-size:12px;line-height:1.5;color:#6B6F66;text-align:center;">Não criou uma conta no ReconPay? Ignore este e-mail: nada será ativado.</p>
                <p style="margin:0;font-size:12px;font-weight:600;color:#6B6F66;text-align:center;">ReconPay · Conciliação financeira</p>
                </td></tr>
                </table>
                </body>
                </html>
                """.formatted(SUBJECT, FONT, escapeHtml(recipientName), escapeHtml(toEmail), href, expiry, escapeHtml(brandMark));
    }

    private static String text(String toEmail, String recipientName, String link, String expiry) {
        return """
                Olá, %s!

                Falta um passo para ativar sua conta no ReconPay. Confirme que %s é seu abrindo o endereço abaixo:

                %s

                %s

                Não criou uma conta no ReconPay? Ignore este e-mail: nada será ativado.

                ReconPay · Conciliação financeira
                """.formatted(recipientName, toEmail, link, expiry);
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public record VerificationEmail(String subject, String html, String text) {
    }
}
