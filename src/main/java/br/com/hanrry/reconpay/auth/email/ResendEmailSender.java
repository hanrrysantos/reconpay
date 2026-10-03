package br.com.hanrry.reconpay.auth.email;

import br.com.hanrry.reconpay.auth.config.EmailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "reconpay.email.resend-api-key")
public class ResendEmailSender implements EmailSender {

    private static final String RESEND_URL = "https://api.resend.com/emails";

    private final EmailProperties emailProperties;
    private final RestClient restClient = RestClient.create();

    @Override
    public void sendEmailVerification(String toEmail, String recipientName, String rawToken) {
        String verifyHint = emailProperties.verificationBaseUrl()
                + " — use POST /api/auth/verify-email com o token abaixo.";

        Map<String, Object> body = Map.of(
                "from", emailProperties.from(),
                "to", List.of(toEmail),
                "subject", "Confirme seu e-mail — ReconPay",
                "html", """
                        <p>Olá, %s!</p>
                        <p>Confirme seu e-mail para ativar sua conta no ReconPay.</p>
                        <p><strong>Token:</strong> %s</p>
                        <p>%s</p>
                        """.formatted(escapeHtml(recipientName), rawToken, escapeHtml(verifyHint))
        );

        restClient.post()
                .uri(RESEND_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + emailProperties.resendApiKey())
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("Verification email dispatched via Resend to {}", toEmail);
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
