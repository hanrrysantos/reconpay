package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.demo.config.DemoProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DemoRateLimiterTest {

    private static final Instant START = Instant.parse("2026-10-20T12:00:00Z");

    private final MutableClock clock = new MutableClock(START);
    private final DemoRateLimiter limiter = new DemoRateLimiter(clock,
            new DemoProperties(Duration.ofHours(24), 100, 2, 2, 10, 900000, Duration.ofSeconds(30)));

    @Test
    void thirdCreationFromTheSameAddressIsBlockedWhileAnotherAddressIsNot() {
        assertThat(limiter.isAllowed("1.1.1.1")).isTrue();
        limiter.record("1.1.1.1");
        assertThat(limiter.isAllowed("1.1.1.1")).isTrue();
        limiter.record("1.1.1.1");

        assertThat(limiter.isAllowed("1.1.1.1")).isFalse();
        assertThat(limiter.isAllowed("2.2.2.2")).isTrue();
    }

    @Test
    void addressIsAllowedAgainSixtyMinutesAfterItsFirstCreationButNotOneSecondEarlier() {
        limiter.record("1.1.1.1");
        clock.set(START.plus(Duration.ofMinutes(10)));
        limiter.record("1.1.1.1");

        clock.set(START.plus(Duration.ofMinutes(59)).plusSeconds(59));
        assertThat(limiter.isAllowed("1.1.1.1")).isFalse();

        clock.set(START.plus(Duration.ofMinutes(60)));
        assertThat(limiter.isAllowed("1.1.1.1")).isTrue();
    }

    @Test
    void addressWhoseCreationsAllLeftTheWindowStartsFromZero() {
        limiter.record("1.1.1.1");
        limiter.record("1.1.1.1");

        clock.set(START.plus(Duration.ofMinutes(61)));
        assertThat(limiter.isAllowed("1.1.1.1")).isTrue();
        limiter.record("1.1.1.1");
        assertThat(limiter.isAllowed("1.1.1.1")).isTrue();
        limiter.record("1.1.1.1");
        assertThat(limiter.isAllowed("1.1.1.1")).isFalse();
    }

    @Test
    void checkingWithoutRecordingDoesNotUseTheQuota() {
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.isAllowed("1.1.1.1")).isTrue();
        }
        limiter.record("1.1.1.1");
        assertThat(limiter.isAllowed("1.1.1.1")).isTrue();
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void set(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
