package br.com.hanrry.reconpay.auth.email;

import br.com.hanrry.reconpay.auth.config.EmailProperties;
import br.com.hanrry.reconpay.auth.email.VerificationEmailComposer.VerificationEmail;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;


import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendEmailSenderTest {

    @Test
    void sendsSubjectHtmlAndPlainTextFromTheComposer() {
        EmailProperties properties = new EmailProperties("re_key", "ReconPay <noreply@hanrry.top>", "https://app.example.com", 24L);
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
                .andRespond(withSuccess("{\"id\":\"1\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        new ResendEmailSender(properties, composer, builder).sendEmailVerification("ana@lojacentro.com.br", "Ana", "tok");

        server.verify();
    }
}
