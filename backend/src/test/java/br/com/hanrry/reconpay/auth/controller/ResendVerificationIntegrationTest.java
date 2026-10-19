package br.com.hanrry.reconpay.auth.controller;

import br.com.hanrry.reconpay.auth.email.CapturingEmailSender;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ResendVerificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CapturingEmailSender capturingEmailSender;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearEmails() {
        capturingEmailSender.clear();
    }

    @Test
    void resendIssuesNewLinkAndInvalidatesThePreviousOne() throws Exception {
        register("reenvio@test.local");
        String oldToken = capturingEmailSender.lastEmail().rawToken();
        ageTokensOf("reenvio@test.local");
        capturingEmailSender.clear();

        resend("reenvio@test.local")
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        var captured = capturingEmailSender.lastEmail();
        assertThat(captured).isNotNull();
        assertThat(captured.toEmail()).isEqualTo("reenvio@test.local");
        assertThat(captured.rawToken()).isNotEqualTo(oldToken);
        verify(oldToken).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        verify(captured.rawToken()).andExpect(status().isNoContent());
    }

    @Test
    void resendAnswersTheSameForUnknownEmailAndSendsNothing() throws Exception {
        resend("ninguem@test.local")
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        assertThat(capturingEmailSender.lastEmail()).isNull();
    }

    @Test
    void resendWithinCooldownAnswersAcceptedAndSendsNothing() throws Exception {
        register("apressado@test.local");
        capturingEmailSender.clear();

        resend("apressado@test.local")
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        assertThat(capturingEmailSender.lastEmail()).isNull();
    }

    @Test
    void resendRejectsBlankAndInvalidEmail() throws Exception {
        for (String email : new String[]{"", "nao-e-email"}) {
            resend(email)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
        assertThat(capturingEmailSender.lastEmail()).isNull();
    }

    private ResultActions resend(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\"}".formatted(email)));
    }

    private ResultActions verify(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\": \"%s\"}".formatted(token)));
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Usuario Pendente", "email": "%s", "password": "Analista@123"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private void ageTokensOf(String email) {
        jdbcTemplate.update("""
                UPDATE email_verification_tokens SET created_at = created_at - INTERVAL '2 minutes'
                WHERE user_id = (SELECT id FROM users WHERE email = ?)
                """, email);
    }
}
