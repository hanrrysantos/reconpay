package br.com.hanrry.reconpay.auth.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UserAdministrationIntegrationTest extends AbstractIntegrationTest {

    private static final String BEARER = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserRepository userRepository;

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
