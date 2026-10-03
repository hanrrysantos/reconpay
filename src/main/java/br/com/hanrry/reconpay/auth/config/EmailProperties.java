package br.com.hanrry.reconpay.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "reconpay.email")
public record EmailProperties(
        String resendApiKey,
        String from,
        String verificationBaseUrl,
        Long verificationTokenHours
) {
    public EmailProperties {
        if (from == null || from.isBlank()) {
            from = "ReconPay <noreply@reconpay.local>";
        }
        if (verificationBaseUrl == null || verificationBaseUrl.isBlank()) {
            verificationBaseUrl = "http://localhost:8080";
        }
        if (verificationTokenHours == null || verificationTokenHours <= 0) {
            verificationTokenHours = 24L;
        }
    }
}
