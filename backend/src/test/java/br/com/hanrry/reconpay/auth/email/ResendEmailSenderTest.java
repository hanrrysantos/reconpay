package br.com.hanrry.reconpay.auth.email;

import br.com.hanrry.reconpay.auth.config.EmailProperties;
import br.com.hanrry.reconpay.auth.email.VerificationEmailComposer.VerificationEmail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(OutputCaptureExtension.class)
class ResendEmailSenderTest {

    @Test
    void sendsSubjectHtmlAndPlainTextFromTheComposer() {
        EmailProperties properties = new EmailProperties("re_key", "ReconPay <noreply@hanrry.top>", "https://app.example.com", 24L, null);
        VerificationEmailComposer composer = new VerificationEmailComposer(new EmailVerificationLinkFactory(properties), properties);
        VerificationEmail expected = composer.compose("ana@lojacentro.com.br", "Ana", "tok");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_key"))
                .andExpect(jsonPath("$.from").value("ReconPay <noreply@hanrry.top>"))
                .andExpect(jsonPath("$['to'][0]").value("ana@lojacentro.com.br"))
                .andExpect(jsonPath("$.subject").value("Confirme seu e-mail — ReconPay"))
                .andExpect(jsonPath("$.html").value(expected.html()))
                .andExpect(jsonPath("$.text").value(expected.text()))
                .andRespond(withSuccess("{\"id\":\"1\"}", MediaType.APPLICATION_JSON));

        new ResendEmailSender(properties, composer, builder).sendEmailVerification("ana@lojacentro.com.br", "Ana", "tok");

        server.verify();
    }

    private static EmailProperties properties(String redirectTo) {
        return new EmailProperties("re_key", "ReconPay <noreply@hanrry.top>", "https://app.example.com", 24L, redirectTo);
    }

    private static void send(EmailProperties properties, String toEmail, String expectedTo, String expectedSubject) {
        VerificationEmailComposer composer = new VerificationEmailComposer(new EmailVerificationLinkFactory(properties), properties);
        VerificationEmail expected = composer.compose(toEmail, "Ana", "tok");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(jsonPath("$.to.length()").value(1))
                .andExpect(jsonPath("$['to'][0]").value(expectedTo))
                .andExpect(jsonPath("$.subject").value(expectedSubject))
                .andExpect(jsonPath("$.html").value(expected.html()))
                .andExpect(jsonPath("$.text").value(expected.text()))
                .andRespond(withSuccess("{\"id\":\"1\"}", MediaType.APPLICATION_JSON));

        new ResendEmailSender(properties, composer, builder).sendEmailVerification(toEmail, "Ana", "tok");

        server.verify();
    }

    @Test
    void redirectSendsOnlyToTheRedirectAddressWithTheOriginalContent() {
        send(properties("dev@example.com"), "ana@lojacentro.com.br", "dev@example.com",
                "[dev → ana@lojacentro.com.br] Confirme seu e-mail — ReconPay");
    }

    @Test
    void redirectedEmailKeepsTheRegisteredAddressAndLinkInTheBody() {
        EmailProperties properties = properties("dev@example.com");
        VerificationEmail composed = new VerificationEmailComposer(new EmailVerificationLinkFactory(properties), properties)
                .compose("ana@lojacentro.com.br", "Ana", "tok");

        assertThat(composed.text())
                .contains("Confirme que ana@lojacentro.com.br é seu")
                .contains("https://app.example.com/verificar-email?token=tok")
                .doesNotContain("dev@example.com");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void withoutRedirectSendsToTheRegisteredAddressWithTheUsualSubject(String redirectTo) {
        send(properties(redirectTo), "ana@lojacentro.com.br", "ana@lojacentro.com.br", "Confirme seu e-mail — ReconPay");
    }

    @Test
    void redirectAddressIsTrimmed() {
        send(properties("  dev@example.com  "), "ana@lojacentro.com.br", "dev@example.com",
                "[dev → ana@lojacentro.com.br] Confirme seu e-mail — ReconPay");
    }

    @Test
    void redirectToTheSameAddressSendsOnceWithThePrefix() {
        send(properties("ana@lojacentro.com.br"), "ana@lojacentro.com.br", "ana@lojacentro.com.br",
                "[dev → ana@lojacentro.com.br] Confirme seu e-mail — ReconPay");
    }

    @Test
    void redirectLogsTheRegisteredAndTheDestinationAddresses(CapturedOutput output) {
        send(properties("dev@example.com"), "ana@lojacentro.com.br", "dev@example.com",
                "[dev → ana@lojacentro.com.br] Confirme seu e-mail — ReconPay");

        assertThat(output).contains("ana@lojacentro.com.br").contains("redirected to dev@example.com");
    }
}
