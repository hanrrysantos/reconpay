package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.auth.dto.AuthResponseDTO;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.demo.config.DemoProperties;
import br.com.hanrry.reconpay.exception.DemoRateLimitException;
import br.com.hanrry.reconpay.exception.DemoUnavailableException;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.security.JwtService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DemoAccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final String IP = "203.0.113.7";
    private static final DemoAccount ACCOUNT =
            new DemoAccount(UUID.randomUUID(), "demo-0123456789ab@demo.reconpay.invalid", UUID.randomUUID());

    private final DemoRateLimiter rateLimiter = mock(DemoRateLimiter.class);
    private final IUserRepository userRepository = mock(IUserRepository.class);
    private final DemoProvisioner provisioner = mock(DemoProvisioner.class);
    private final DemoRunExecutor runExecutor = mock(DemoRunExecutor.class);
    private final DemoPurger purger = mock(DemoPurger.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuditLogger auditLogger = mock(AuditLogger.class);
    private final DemoAccountService service = new DemoAccountService(rateLimiter, userRepository, provisioner,
            runExecutor, purger, jwtService,
            new DemoProperties(Duration.ofHours(24), 100, 2, 2, 10, 900000, Duration.ofSeconds(30)),
            Clock.fixed(NOW, ZoneOffset.UTC), auditLogger);

    @Test
    void blockedAddressIsRefusedWithoutProvisioning() {
        when(rateLimiter.isAllowed(IP)).thenReturn(false);

        assertThatThrownBy(() -> service.create(IP))
                .isInstanceOf(DemoRateLimitException.class)
                .hasMessage("Limite de demonstrações atingido. Tente novamente em até 1 hora.");

        verifyNoInteractions(provisioner, runExecutor, jwtService);
        verify(rateLimiter, never()).record(anyString());
    }

    @Test
    void hundredActiveDemosRefuseTheNextWithoutProvisioning() {
        when(rateLimiter.isAllowed(IP)).thenReturn(true);
        when(userRepository.countByDemoExpiresAtAfter(NOW)).thenReturn(100L);

        assertThatThrownBy(() -> service.create(IP))
                .isInstanceOf(DemoUnavailableException.class)
                .hasMessage("Demonstrações esgotadas no momento. Tente novamente mais tarde.");

        verifyNoInteractions(provisioner, runExecutor, jwtService);
        verify(rateLimiter, never()).record(anyString());
    }

    @Test
    void ninetyNineActiveDemosStillCreateOneAndCountTheAddress() {
        succeedingCreation(99L, 604800L);

        AuthResponseDTO response = service.create(IP);

        assertThat(response.token()).isEqualTo("demo-token");
        assertThat(response.type()).isEqualTo("Bearer");
        verify(runExecutor).complete(ACCOUNT.runId());
        verify(rateLimiter).record(IP);
        verify(purger, never()).purge(any());
    }

    @Test
    void tokenLastsTheDemoLifetimeWhenTheJwtLifetimeIsLonger() {
        succeedingCreation(0L, 604800L);

        AuthResponseDTO response = service.create(IP);

        assertThat(response.expiresIn()).isEqualTo(86400L);
        verify(jwtService).generateToken(ACCOUNT.email(), Duration.ofHours(24));
    }

    @Test
    void tokenLastsTheJwtLifetimeWhenItIsShorterThanTheDemo() {
        succeedingCreation(0L, 3600L);

        AuthResponseDTO response = service.create(IP);

        assertThat(response.expiresIn()).isEqualTo(3600L);
        verify(jwtService).generateToken(ACCOUNT.email(), Duration.ofHours(1));
    }

    @Test
    void provisioningFailureNeedsNoPurgeAndDoesNotCountTheAddress() {
        when(rateLimiter.isAllowed(IP)).thenReturn(true);
        IllegalStateException failure = new IllegalStateException("import broke");
        when(provisioner.provision(NOW)).thenThrow(failure);

        assertThatThrownBy(() -> service.create(IP)).isSameAs(failure);

        verifyNoInteractions(purger, runExecutor, jwtService);
        verify(rateLimiter, never()).record(anyString());
    }

    @Test
    void runFailurePurgesTheCommittedDemoAndDoesNotCountTheAddress() {
        when(rateLimiter.isAllowed(IP)).thenReturn(true);
        when(provisioner.provision(NOW)).thenReturn(ACCOUNT);
        IllegalStateException failure = new IllegalStateException("Conciliação da demonstração falhou");
        doThrow(failure).when(runExecutor).complete(ACCOUNT.runId());

        assertThatThrownBy(() -> service.create(IP)).isSameAs(failure);

        verify(purger).purge(ACCOUNT.userId());
        verify(rateLimiter, never()).record(anyString());
        verifyNoInteractions(jwtService);
    }

    private void succeedingCreation(long activeDemos, long jwtExpirationSeconds) {
        when(rateLimiter.isAllowed(IP)).thenReturn(true);
        when(userRepository.countByDemoExpiresAtAfter(NOW)).thenReturn(activeDemos);
        when(provisioner.provision(NOW)).thenReturn(ACCOUNT);
        when(jwtService.getExpirationSeconds()).thenReturn(jwtExpirationSeconds);
        when(jwtService.generateToken(any(), any(Duration.class))).thenReturn("demo-token");
    }
}
