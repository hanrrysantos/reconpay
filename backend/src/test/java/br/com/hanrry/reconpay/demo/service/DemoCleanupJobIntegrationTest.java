package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DemoCleanupJobIntegrationTest extends AbstractIntegrationTest {

    private static final ShiftedClock CLOCK = new ShiftedClock();

    @TestConfiguration
    static class ShiftedClockConfig {
        @Bean
        @Primary
        Clock shiftedClock() {
            return CLOCK;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoCleanupJob job;

    @MockitoSpyBean
    private DemoPurger purger;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logs.list = new CopyOnWriteArrayList<>();
        logs.start();
        ((Logger) LoggerFactory.getLogger(DemoCleanupJob.class)).addAppender(logs);
    }

    @AfterEach
    void restore() {
        ((Logger) LoggerFactory.getLogger(DemoCleanupJob.class)).detachAppender(logs);
        CLOCK.shift(Duration.ZERO);
    }

    @Test
    void aDayLaterTheExpiredDemoAndItsDataAreGoneAndTheActiveOneStays() throws Exception {
        // The suite shares one database: clear whatever would also be expired a day from now.
        CLOCK.shift(Duration.ofHours(24));
        job.purgeExpired();
        CLOCK.shift(Duration.ZERO);

        UUID demo = createDemo();
        UUID merchantId = jdbc.queryForObject(
                "SELECT merchant_id FROM user_merchants WHERE user_id = ?", UUID.class, demo);
        assertThat(rowsOf(merchantId)).isPositive();
        UUID active = insertDemoUser(Instant.now().plus(Duration.ofHours(48)));
        logs.list.clear();

        CLOCK.shift(Duration.ofHours(24));
        job.purgeExpired();

        assertThat(userRepository.existsById(demo)).isFalse();
        assertThat(rowsOf(merchantId)).isZero();
        assertThat(userRepository.existsById(active)).isTrue();
        assertThat(logs.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage())
                    .isEqualTo("Limpeza de demonstrações concluída | usuarios=1 merchants=1");
        });

        jdbc.update("DELETE FROM users WHERE id = ?", active);
    }

    @Test
    void aFailingDemoDoesNotStopTheNextOne() {
        job.purgeExpired();
        UUID failing = insertDemoUser(Instant.now().minus(Duration.ofHours(2)));
        UUID next = insertDemoUser(Instant.now().minus(Duration.ofHours(1)));
        doThrow(new IllegalStateException("purge failed")).when(purger).purge(failing);
        logs.list.clear();

        job.purgeExpired();

        assertThat(userRepository.existsById(next)).isFalse();
        assertThat(userRepository.existsById(failing)).isTrue();
        assertThat(logs.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("userId=" + failing);
        });
        assertThat(logs.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage())
                    .isEqualTo("Limpeza de demonstrações concluída | usuarios=1 merchants=0");
        });

        jdbc.update("DELETE FROM users WHERE id = ?", failing);
    }

    @Test
    void theSweepRunsEveryFifteenMinutesByDefault() throws Exception {
        Scheduled scheduled = DemoCleanupJob.class.getMethod("purgeExpired").getAnnotation(Scheduled.class);

        assertThat(scheduled.fixedDelayString()).isEqualTo("${reconpay.demo.cleanup-delay-ms:900000}");
    }

    private UUID createDemo() throws Exception {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String ip = "10.%d.%d.%d".formatted(random.nextInt(256), random.nextInt(256), random.nextInt(1, 255));
        String created = mockMvc.perform(post("/api/auth/demo").with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                }))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String me = mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + JsonPath.read(created, "$.token")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(me, "$.id"));
    }

    // Merchant rows plus the scenario data hanging from it.
    private long rowsOf(UUID merchantId) {
        long total = 0;
        for (String table : List.of("merchants", "fee_rules", "internal_transactions", "external_settlements",
                "bank_statement_lines", "reconciliation_runs")) {
            String column = table.equals("merchants") ? "id" : "merchant_id";
            total += jdbc.queryForObject(
                    "SELECT count(*) FROM " + table + " WHERE " + column + " = ?", Long.class, merchantId);
        }
        return total;
    }

    private UUID insertDemoUser(Instant demoExpiresAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, name, email, password, role, active, created_at, updated_at, demo_expires_at)
                VALUES (?, 'Visitante', ?, 'not-used', 'OPERATOR', true, now(), now(), ?)
                """, id, "demo-cleanup-" + id + "@demo.reconpay.invalid", Timestamp.from(demoExpiresAt));
        return id;
    }

    /* Real time moved by an offset, so "a day later" is one call away. */
    private static final class ShiftedClock extends Clock {

        private volatile Duration offset = Duration.ZERO;

        private void shift(Duration offset) {
            this.offset = offset;
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
            return Instant.now().plus(offset);
        }
    }
}
