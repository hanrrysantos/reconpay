package br.com.hanrry.reconpay.auth.service;

import br.com.hanrry.reconpay.auth.dto.AuthRequestDTO;
import br.com.hanrry.reconpay.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.AuthenticationManager;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtExpiryTest {
    @Test void loginMetadataMatchesConfiguredSignedTokenLifetime() {
        String secret = "test-secret-key-with-at-least-32-characters";
        var jwt = new JwtService();
        ReflectionTestUtils.setField(jwt, "secret", secret);
        ReflectionTestUtils.setField(jwt, "expiration", 123L);
        var auth = new AuthService(null, null, null, mock(AuthenticationManager.class), jwt);
        var response = auth.login(new AuthRequestDTO("analyst@example.com", "password"));
        var claims = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(response.token()).getPayload();
        assertThat(claims.getExpiration().toInstant().getEpochSecond() - claims.getIssuedAt().toInstant().getEpochSecond()).isEqualTo(123);
        assertThat(response.expiresIn()).isEqualTo(123);
    }
}
