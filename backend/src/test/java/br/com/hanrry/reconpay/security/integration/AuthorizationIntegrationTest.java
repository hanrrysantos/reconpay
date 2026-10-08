package br.com.hanrry.reconpay.security.integration;

import br.com.hanrry.reconpay.auth.email.CapturingEmailSender;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CapturingEmailSender capturingEmailSender;

    private String operatorToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        operatorToken = IntegrationTestUtils.obtainOperatorToken(mockMvc);
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
    }

    @Test
    void operatorShouldListMerchantsWithoutForbidden() throws Exception {
        mockMvc.perform(get("/api/merchants")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void operatorShouldBeForbiddenOnUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void unauthenticatedRequestShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/merchants"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void selfRegisteredUserShouldNotAuthenticateBeforeEmailVerification() throws Exception {
        capturingEmailSender.clear();
        register("pendente@test.local");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload("pendente@test.local")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Confirme seu e-mail para ativar sua conta"));
    }

    @Test
    void selfRegisteredUserShouldAuthenticateAfterEmailVerification() throws Exception {
        capturingEmailSender.clear();
        register("aprovado@test.local");

        var captured = capturingEmailSender.lastEmail();
        org.assertj.core.api.Assertions.assertThat(captured).isNotNull();
        org.assertj.core.api.Assertions.assertThat(captured.toEmail()).isEqualTo("aprovado@test.local");

        mockMvc.perform(get("/api/auth/verify-email")
                        .param("token", captured.rawToken()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("E-mail confirmado")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload("aprovado@test.local")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void verifyEmailWithInvalidTokenShouldReturnValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "totally-invalid-token"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void verifyShouldFailAfterAdminDeactivatesPendingUser() throws Exception {
        capturingEmailSender.clear();
        String email = "revogado@test.local";
        String userId = register(email);
        var captured = capturingEmailSender.lastEmail();
        org.assertj.core.api.Assertions.assertThat(captured).isNotNull();

        mockMvc.perform(delete("/api/users/{id}", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s"
                                }
                                """.formatted(captured.rawToken())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void analystShouldNotActivateUsers() throws Exception {
        String userId = register("negado@test.local");

        mockMvc.perform(patch("/api/users/{id}/activation", userId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
    }

    private String register(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Usuario Pendente",
                                  "email": "%s",
                                  "password": "Analista@123"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private String loginPayload(String email) {
        return """
                {
                  "email": "%s",
                  "password": "Analista@123"
                }
                """.formatted(email);
    }
}
