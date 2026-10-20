package br.com.hanrry.reconpay.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

/* Deletes expired demo accounts. Each demo goes in its own transaction, so one failure keeps the rest. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoCleanupJob {

    private final DemoPurger purger;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${reconpay.demo.cleanup-delay-ms:900000}")
    public void purgeExpired() {
        int users = 0;
        int merchants = 0;
        for (UUID userId : purger.findExpired(clock.instant())) {
            try {
                merchants += purger.purge(userId);
                users++;
            } catch (RuntimeException ex) {
                log.warn("Falha ao apagar demonstração expirada | userId={}", userId, ex);
            }
        }
        log.info("Limpeza de demonstrações concluída | usuarios={} merchants={}", users, merchants);
    }
}
