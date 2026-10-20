package br.com.hanrry.reconpay.auth.email;

import br.com.hanrry.reconpay.auth.config.EmailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "reconpay.email.resend-api-key")
public class ResendEmailSender implements EmailSender {

    private static final String RESEND_URL = "https://api.resend.com/emails";

    private final EmailProperties emailProperties;
    private final VerificationEmailComposer verificationEmailComposer;
    private final RestClient restClient;
    private final String redirectTo;

    public ResendEmailSender(EmailProperties emailProperties,
                             VerificationEmailComposer verificationEmailComposer,
                             RestClient.Builder restClientBuilder,
                             Environment environment) {
        this.emailProperties = emailProperties;
        this.verificationEmailComposer = verificationEmailComposer;
        this.restClient = restClientBuilder.build();
        // Relaxed binding fills redirectTo from an OS variable in any profile, so the dev check lives here.
        this.redirectTo = environment.acceptsProfiles(Profiles.of("dev")) ? emailProperties.redirectTo() : null;
    }

    @Override
    public void sendEmailVerification(String toEmail, String recipientName, String rawToken) {
        VerificationEmailComposer.VerificationEmail email = verificationEmailComposer.compose(toEmail, recipientName, rawToken);
        String recipient = redirectTo == null ? toEmail : redirectTo;
        String subject = redirectTo == null ? email.subject() : "[dev → " + toEmail + "] " + email.subject();

        Map<String, Object> body = Map.of(
                "from", emailProperties.from(),
                "to", List.of(recipient),
                "subject", subject,
                "html", email.html(),
                "text", email.text()
        );

        restClient.post()
                .uri(RESEND_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + emailProperties.resendApiKey())
                .body(body)
                .retrieve()
                .toBodilessEntity();

        if (redirectTo == null) {
            log.info("Verification email dispatched via Resend to {}", toEmail);
        } else {
            log.info("Verification email for {} dispatched via Resend, redirected to {}", toEmail, redirectTo);
        }
    }
}
