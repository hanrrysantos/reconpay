package br.com.hanrry.reconpay.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-characters";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 604800L);
    }

    @Test
    void generateTokenWithTtlShouldLastExactlyTheGivenDuration() {
        Claims claims = parse(jwtService.generateToken("demo@demo.reconpay.invalid", Duration.ofHours(24)));

        assertThat(lifetimeSeconds(claims)).isEqualTo(86400);
        assertThat(claims.getSubject()).isEqualTo("demo@demo.reconpay.invalid");
    }

    @Test
    void generateTokenWithoutTtlShouldKeepTheConfiguredExpiration() {
        Claims claims = parse(jwtService.generateToken("analyst@example.com"));

        assertThat(lifetimeSeconds(claims)).isEqualTo(604800);
        assertThat(claims.getSubject()).isEqualTo("analyst@example.com");
    }

    private static Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static long lifetimeSeconds(Claims claims) {
        return claims.getExpiration().toInstant().getEpochSecond() - claims.getIssuedAt().toInstant().getEpochSecond();
    }
}
