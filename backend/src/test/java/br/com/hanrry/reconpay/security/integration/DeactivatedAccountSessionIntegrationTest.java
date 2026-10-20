package br.com.hanrry.reconpay.security.integration;

import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DeactivatedAccountSessionIntegrationTest extends AbstractIntegrationTest {

    private static final String BEARER = "Bearer ";
    private static final String PASSWORD = "Operadora1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String email;
    private UUID operatorId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);

        UserEntity operator = new UserEntity();
        operator.setName("Operadora Sessao");
        operator.setEmail("sessao-" + UUID.randomUUID() + "@test.local");
        operator.setPassword(passwordEncoder.encode(PASSWORD));
        operator.setRole(UserRole.OPERATOR);
        operator.setActive(true);
        operator = userRepository.save(operator);
        email = operator.getEmail();
        operatorId = operator.getId();
    }

    @Test
    void tokenIssuedBeforeDeactivationShouldBeUnauthorized() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, BEARER + token))
                .andExpect(status().isOk());

        deactivate();

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, BEARER + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/merchants").header(HttpHeaders.AUTHORIZATION, BEARER + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void reactivationShouldRestoreTheOldTokenAndThePasswordLogin() throws Exception {
        String token = login();
        deactivate();

        mockMvc.perform(patch("/api/users/{id}/activation", operatorId)
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, BEARER + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        login();
    }

    @Test
    void loginOfADeactivatedAccountShouldStillAskForEmailConfirmation() throws Exception {
        deactivate();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Confirme seu e-mail para ativar sua conta"));
    }

    private void deactivate() throws Exception {
        mockMvc.perform(delete("/api/users/{id}", operatorId)
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isNoContent());
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String loginBody() {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, PASSWORD);
    }
}
