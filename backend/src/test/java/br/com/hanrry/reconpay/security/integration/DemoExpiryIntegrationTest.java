package br.com.hanrry.reconpay.security.integration;

import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DemoExpiryIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void expiredDemoTokenShouldBeUnauthorized() throws Exception {
        String token = tokenFor(NOW.minus(Duration.ofHours(1)));

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void demoExpiringAtTheCurrentInstantShouldBeUnauthorized() throws Exception {
        String token = tokenFor(NOW);

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void demoExpiringOneSecondAfterNowShouldStillAuthenticate() throws Exception {
        String email = saveUser(NOW.plusSeconds(1));

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + jwtService.generateToken(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void regularAccountWithoutDemoExpiryShouldAuthenticate() throws Exception {
        String email = saveUser(null);

        mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + jwtService.generateToken(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    private String tokenFor(Instant demoExpiresAt) {
        return jwtService.generateToken(saveUser(demoExpiresAt));
    }

    private String saveUser(Instant demoExpiresAt) {
        UserEntity user = new UserEntity();
        user.setName("Visitante");
        user.setEmail("demo-" + UUID.randomUUID() + "@demo.reconpay.invalid");
        user.setPassword("not-used");
        user.setRole(UserRole.OPERATOR);
        user.setActive(true);
        user.setDemoExpiresAt(demoExpiresAt);
        return userRepository.save(user).getEmail();
    }
}
