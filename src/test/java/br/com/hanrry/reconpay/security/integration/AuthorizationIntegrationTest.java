package br.com.hanrry.reconpay.security.integration;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthorizationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String analystToken;

    @BeforeEach
    void setUp() throws Exception {
        analystToken = IntegrationTestUtils.obtainAnalystToken(mockMvc);
    }

    @Test
    void financialAnalystShouldBeForbiddenOnMerchants() throws Exception {
        mockMvc.perform(get("/api/merchants")
                        .header("Authorization", "Bearer " + analystToken))
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
    void selfRegisteredUserShouldNotAuthenticateBeforeActivation() throws Exception {
        register("pendente@test.local");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload("pendente@test.local")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void selfRegisteredUserShouldAuthenticateAfterAdminActivation() throws Exception {
        String userId = register("aprovado@test.local");
        String adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);

        mockMvc.perform(patch("/api/users/{id}/activation", userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload("aprovado@test.local")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void analystShouldNotActivateUsers() throws Exception {
        String userId = register("negado@test.local");

        mockMvc.perform(patch("/api/users/{id}/activation", userId)
                        .header("Authorization", "Bearer " + analystToken))
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
