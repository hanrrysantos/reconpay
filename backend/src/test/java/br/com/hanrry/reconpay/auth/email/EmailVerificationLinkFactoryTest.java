package br.com.hanrry.reconpay.auth.email;

import br.com.hanrry.reconpay.auth.config.EmailProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationLinkFactoryTest {

    @Test
    void linkPointsToTheAppVerificationPageByDefault() {
        EmailVerificationLinkFactory factory = new EmailVerificationLinkFactory(new EmailProperties(null, null, null, null));

        assertThat(factory.buildConfirmUrl("q7Xk2mV9pL4r"))
                .isEqualTo("http://localhost:3000/verificar-email?token=q7Xk2mV9pL4r");
    }

    @Test
    void linkIgnoresTrailingSlashesInTheBaseUrl() {
        EmailVerificationLinkFactory factory = new EmailVerificationLinkFactory(
                new EmailProperties(null, null, "https://app.example.com//", null));

        assertThat(factory.buildConfirmUrl("abc-_123"))
                .isEqualTo("https://app.example.com/verificar-email?token=abc-_123");
    }
}
