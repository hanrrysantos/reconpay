package br.com.hanrry.reconpay.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "reconpay.email")
public record EmailProperties(
        String resendApiKey,
        String from,
        String verificationBaseUrl,
        Long verificationTokenHours,
        String redirectTo
) {
    public EmailProperties {
        if (from == null || from.isBlank()) {
            from = "ReconPay <noreply@reconpay.local>";
        }
        if (verificationBaseUrl == null || verificationBaseUrl.isBlank()) {
            verificationBaseUrl = "http://localhost:3000";
        }
        if (verificationTokenHours == null || verificationTokenHours <= 0) {
            verificationTokenHours = 24L;
        }
        // Dev-only: application-dev.yaml is the only place that maps RECONPAY_EMAIL_REDIRECT_TO.
        redirectTo = redirectTo == null || redirectTo.isBlank() ? null : redirectTo.strip();
    }
}
