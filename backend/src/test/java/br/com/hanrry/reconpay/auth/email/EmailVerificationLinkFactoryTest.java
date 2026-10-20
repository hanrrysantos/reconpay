package br.com.hanrry.reconpay.auth.email;

import br.com.hanrry.reconpay.auth.config.EmailProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationLinkFactoryTest {

    @Test
    void linkPointsToTheAppVerificationPageByDefault() {
        EmailVerificationLinkFactory factory = new EmailVerificationLinkFactory(new EmailProperties(null, null, null, null, null));

        assertThat(factory.buildConfirmUrl("q7Xk2mV9pL4r"))
                .isEqualTo("http://localhost:3000/verificar-email?token=q7Xk2mV9pL4r");
    }

    @Test
    void linkIgnoresTrailingSlashesInTheBaseUrl() {
        EmailVerificationLinkFactory factory = new EmailVerificationLinkFactory(
                new EmailProperties(null, null, "https://app.example.com//", null, null));

        assertThat(factory.buildConfirmUrl("abc-_123"))
                .isEqualTo("https://app.example.com/verificar-email?token=abc-_123");
    }

    @Test
    void brandMarkIsServedByTheAppByDefault() {
        EmailVerificationLinkFactory factory = new EmailVerificationLinkFactory(new EmailProperties(null, null, null, null, null));

        assertThat(factory.buildBrandMarkUrl())
                .isEqualTo("http://localhost:3000/brand/reconpay-app-icon.png");
    }

    @Test
    void brandMarkIgnoresTrailingSlashesInTheBaseUrl() {
        EmailVerificationLinkFactory factory = new EmailVerificationLinkFactory(
                new EmailProperties(null, null, "https://app.example.com//", null, null));

        assertThat(factory.buildBrandMarkUrl())
                .isEqualTo("https://app.example.com/brand/reconpay-app-icon.png");
    }
}
