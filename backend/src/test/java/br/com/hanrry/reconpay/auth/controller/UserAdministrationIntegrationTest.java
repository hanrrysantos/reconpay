package br.com.hanrry.reconpay.auth.controller;

import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.entity.UserMerchantAccessEntity;
import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.auth.repository.IUserMerchantAccessRepository;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import br.com.hanrry.reconpay.util.TestCnpj;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UserAdministrationIntegrationTest extends AbstractIntegrationTest {

    private static final String BEARER = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IUserMerchantAccessRepository accessRepository;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
    }

    @Test
    void listingShouldFilterByStatusAndDemoAccounts() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String activeRegular = saveUser("Ana " + tag, tag, true, null);
        String inactiveRegular = saveUser("Bruno " + tag, tag, false, null);
        String activeDemo = saveUser("Carla " + tag, tag, true, Instant.now().plus(Duration.ofHours(24)));
        String inactiveDemo = saveUser("Davi " + tag, tag, false, Instant.now().plus(Duration.ofHours(24)));

        assertThat(listedEmails(get("/api/users"), tag))
                .containsExactly(activeRegular);
        assertThat(listedEmails(get("/api/users").param("status", "INACTIVE"), tag))
                .containsExactly(inactiveRegular);
        assertThat(listedEmails(get("/api/users").param("status", "ALL"), tag))
                .containsExactly(activeRegular, inactiveRegular);
        assertThat(listedEmails(get("/api/users").param("status", "ALL").param("demo", "true"), tag))
                .containsExactly(activeRegular, inactiveRegular, activeDemo, inactiveDemo);
        assertThat(listedEmails(get("/api/users").param("status", "ACTIVE").param("demo", "true"), tag))
                .containsExactly(activeRegular, activeDemo);
        assertThat(listedEmails(get("/api/users").param("demo", "false"), tag))
                .containsExactly(activeRegular);
    }

    @Test
    void listingShouldKeepNameOrderingAndTheDefaultPageSize() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String zelia = saveUser("Zelia " + tag, tag, true, null);
        String amanda = saveUser("Amanda " + tag, tag, true, null);
        String marcos = saveUser("Marcos " + tag, tag, true, null);

        assertThat(listedEmails(get("/api/users"), tag)).containsExactly(amanda, marcos, zelia);

        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void listingShouldExposeTheDemoExpiryOfDemoAccounts() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        Instant expiresAt = Instant.parse("2099-01-01T12:00:00Z");
        String demo = saveUser("Eva " + tag, tag, true, expiresAt);

        String body = mockMvc.perform(get("/api/users")
                        .param("demo", "true")
                        .param("size", "2000")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> expiries = JsonPath.read(body, "$.content[?(@.email == '" + demo + "')].demoExpiresAt");
        assertThat(expiries).containsExactly("2099-01-01T12:00:00Z");
    }

    @Test
    void listingShouldRejectAnUnknownStatus() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("status", "FOO")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void adminDeactivatingTheirOwnAccountShouldGetConflictAndStayActive() throws Exception {
        String adminId = userRepository.findByEmail("admin@reconpay.local").orElseThrow().getId().toString();

        mockMvc.perform(delete("/api/users/{id}", adminId)
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Você não pode desativar a própria conta."));

        mockMvc.perform(get("/api/users/{id}", adminId)
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void adminDeactivatingAnotherUserShouldReturnNoContent() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String email = saveUser("Fabio " + tag, tag, true, null);
        UUID id = userRepository.findByEmail(email).orElseThrow().getId();

        mockMvc.perform(delete("/api/users/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(id).orElseThrow().isActive()).isFalse();
    }

    @Test
    void grantingMerchantsToADemoAccountShouldGetConflictAndKeepItsGrants() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        UUID demoId = userRepository.findByEmail(
                saveUser("Gabi " + tag, tag, true, Instant.now().plus(Duration.ofHours(24)))).orElseThrow().getId();
        String ownMerchant = createMerchant();
        String realMerchant = createMerchant();
        accessRepository.save(new UserMerchantAccessEntity(demoId, UUID.fromString(ownMerchant)));

        mockMvc.perform(put("/api/users/{id}/merchants", demoId)
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantIds\":[\"%s\"]}".formatted(realMerchant)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Contas de demonstração não recebem acesso a outras empresas."));

        mockMvc.perform(get("/api/users/{id}/merchants", demoId)
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantIds.length()").value(1))
                .andExpect(jsonPath("$.merchantIds[0]").value(ownMerchant));
    }

    @Test
    void grantingMerchantsToARegularOperatorShouldStillReplaceTheGrants() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        UUID operatorId = userRepository.findByEmail(saveUser("Hugo " + tag, tag, true, null)).orElseThrow().getId();
        String merchant = createMerchant();

        mockMvc.perform(put("/api/users/{id}/merchants", operatorId)
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantIds\":[\"%s\"]}".formatted(merchant)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantIds.length()").value(1))
                .andExpect(jsonPath("$.merchantIds[0]").value(merchant));
    }

    private String createMerchant() throws Exception {
        String body = mockMvc.perform(post("/api/merchants")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Loja Acesso\",\"document\":\"%s\"}".formatted(TestCnpj.random())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private List<String> listedEmails(MockHttpServletRequestBuilder request, String tag) throws Exception {
        String body = mockMvc.perform(request
                        .param("size", "2000")
                        .param("sort", "name,asc")
                        .header(HttpHeaders.AUTHORIZATION, BEARER + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> emails = JsonPath.read(body, "$.content[*].email");
        return emails.stream().filter(email -> email.contains(tag)).toList();
    }

    private String saveUser(String name, String tag, boolean active, Instant demoExpiresAt) {
        UserEntity user = new UserEntity();
        user.setName(name);
        user.setEmail(name.split(" ")[0].toLowerCase() + "-" + tag + "@users.test.local");
        user.setPassword("not-used");
        user.setRole(UserRole.OPERATOR);
        user.setActive(active);
        user.setDemoExpiresAt(demoExpiresAt);
        return userRepository.save(user).getEmail();
    }
}
