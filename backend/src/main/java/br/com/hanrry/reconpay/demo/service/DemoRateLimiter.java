package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.demo.config.DemoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/*
 * Successful demo creations per client address in a sliding hour, kept in memory: a restart
 * starts every address from zero. Only DemoAccountService calls it, under its creation lock,
 * so it needs no synchronization of its own.
 */
@Component
@RequiredArgsConstructor
public class DemoRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);

    private final Map<String, Deque<Instant>> creations = new HashMap<>();
    private final Clock clock;
    private final DemoProperties properties;

    public boolean isAllowed(String ip) {
        Deque<Instant> recent = recent(ip);
        return recent == null || recent.size() < properties.perIpPerHour();
    }

    public void record(String ip) {
        creations.computeIfAbsent(ip, key -> new ArrayDeque<>()).addLast(clock.instant());
    }

    // Drops creations that left the window, and the address once it has none left.
    private Deque<Instant> recent(String ip) {
        Instant windowStart = clock.instant().minus(WINDOW);
        Deque<Instant> recent = creations.get(ip);
        if (recent == null) {
            return null;
        }
        while (!recent.isEmpty() && !recent.peekFirst().isAfter(windowStart)) {
            recent.removeFirst();
        }
        if (recent.isEmpty()) {
            creations.remove(ip);
            return null;
        }
        return recent;
    }
}
