package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.auth.dto.AuthResponseDTO;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.demo.config.DemoProperties;
import br.com.hanrry.reconpay.exception.DemoRateLimitException;
import br.com.hanrry.reconpay.exception.DemoUnavailableException;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/*
 * Creates one demo at a time on this instance, so concurrent requests can never pass the
 * active-demo cap and the VPS builds at most one scenario at once.
 */
@Service
@RequiredArgsConstructor
public class DemoAccountService {

    private final ReentrantLock creationLock = new ReentrantLock(true);

    private final DemoRateLimiter rateLimiter;
    private final IUserRepository userRepository;
    private final DemoProvisioner provisioner;
    private final DemoRunExecutor runExecutor;
    private final DemoPurger purger;
    private final JwtService jwtService;
    private final DemoProperties properties;
    private final Clock clock;
    private final AuditLogger auditLogger;

    public AuthResponseDTO create(String clientIp) {
        creationLock.lock();
        try {
            if (!rateLimiter.isAllowed(clientIp)) {
                throw new DemoRateLimitException();
            }
            Instant now = clock.instant();
            if (userRepository.countByDemoExpiresAtAfter(now) >= properties.maxActive()) {
                throw new DemoUnavailableException();
            }

            // A failed provisioning rolls back on its own; after its commit, compensate by purging.
            DemoAccount account = provisioner.provision(now);
            try {
                runExecutor.complete(account.runId());
            } catch (RuntimeException ex) {
                purger.purge(account.userId());
                throw ex;
            }

            rateLimiter.record(clientIp);
            auditLogger.record("DEMO_CREATED", "user", account.userId());

            Duration ttl = shorter(Duration.ofSeconds(jwtService.getExpirationSeconds()), properties.ttl());
            return new AuthResponseDTO(jwtService.generateToken(account.email(), ttl), "Bearer", ttl.toSeconds());
        } finally {
            creationLock.unlock();
        }
    }

    private static Duration shorter(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }
}
