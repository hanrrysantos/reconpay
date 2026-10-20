package br.com.hanrry.reconpay.demo.controller;

import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.demo.service.DemoPurger;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyIterable;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DemoControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String RATE_LIMITED = "Limite de demonstrações atingido. Tente novamente em até 1 hora.";
    private static final String SOLD_OUT = "Demonstrações esgotadas no momento. Tente novamente mais tarde.";
    private static final List<String> DIVERGENCE_TYPES = List.of(
            "MISSING_SETTLEMENT", "ORPHAN_SETTLEMENT", "INCORRECT_AMOUNT", "FEE_DIVERGENCE", "MISSING_BANK_CREDIT");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private DemoPurger purger;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<UUID> fillers = new ArrayList<>();

    @AfterEach
    void removeFillers() {
        for (UUID filler : fillers) {
            jdbc.update("DELETE FROM users WHERE id = ?", filler);
        }
        fillers.clear();
    }

    @Test
    void anonymousCreationReturnsABearerTokenValidForAtMostOneDay() throws Exception {
        createDemo(nextIp())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(lessThanOrEqualTo(86400)))
                .andExpect(jsonPath("$.expiresIn").value(greaterThan(0)));
    }

    @Test
    void demoTokenSeesOneMerchantWithCompletedRunsAndAnOpenDivergenceOfEachType() throws Exception {
        String token = tokenOf(createDemo(nextIp()).andExpect(status().isCreated()));

        String merchantId = onlyMerchantOf(token);
        String runs = read(token, "/api/merchants/" + merchantId + "/reconciliations")
                .andExpect(jsonPath("$.totalElements").value(greaterThan(0)))
                .andExpect(jsonPath("$.content[*].status", everyItem(is("COMPLETED"))))
                .andExpect(jsonPath("$.content[*].matchedCount", everyItem(greaterThan(0))))
                .andExpect(jsonPath("$.content[*].divergentCount", everyItem(greaterThan(0))))
                .andReturn().getResponse().getContentAsString();
        String runId = JsonPath.read(runs, "$.content[0].id");

        String items = "/api/merchants/" + merchantId + "/reconciliations/" + runId + "/items";
        read(token, items + "?result=MATCHED").andExpect(jsonPath("$.totalElements").value(greaterThan(0)));
        read(token, items + "?result=DIVERGENT").andExpect(jsonPath("$.totalElements").value(greaterThan(0)));
        for (String type : DIVERGENCE_TYPES) {
            read(token, items + "?size=200&discrepancyType=" + type + "&discrepancyStatus=OPEN")
                    .andExpect(jsonPath("$.content[*].discrepancies[?(@.type == '" + type
                            + "' && @.status == 'OPEN')]", not(emptyIterable())));
        }
    }

    @Test
    void twoDemosCreatedTheSameDayHaveTheSameScenario() throws Exception {
        String first = tokenOf(createDemo(nextIp()).andExpect(status().isCreated()));
        String second = tokenOf(createDemo(nextIp()).andExpect(status().isCreated()));

        Scenario firstScenario = scenarioOf(first);
        Scenario secondScenario = scenarioOf(second);

        assertThat(firstScenario.transactions()).isEqualTo(200);
        assertThat(secondScenario).isEqualTo(firstScenario);
    }

    @Test
    void demoTokenCannotReadOrChangeAnotherDemosMerchant() throws Exception {
        String token = tokenOf(createDemo(nextIp()).andExpect(status().isCreated()));
        String otherMerchantId = onlyMerchantOf(tokenOf(createDemo(nextIp()).andExpect(status().isCreated())));

        mockMvc.perform(get("/api/merchants/{id}", otherMerchantId).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mockMvc.perform(get("/api/merchants/{id}/transactions", otherMerchantId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mockMvc.perform(post("/api/merchants/{id}/reconciliations", otherMerchantId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromDate":"2026-07-01","toDate":"2026-07-15"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void thirdCreationFromTheSameAddressWithinAnHourIsRateLimited() throws Exception {
        String ip = nextIp();
        createDemo(ip).andExpect(status().isCreated());
        createDemo(ip).andExpect(status().isCreated());
        long users = userRepository.count();

        createDemo(ip)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.message").value(RATE_LIMITED));

        assertThat(userRepository.count()).isEqualTo(users);
        createDemo(nextIp()).andExpect(status().isCreated());
    }

    @Test
    void hundredActiveDemosRefuseTheNextCreation() throws Exception {
        fillActiveDemosUpTo(100);
        long users = userRepository.count();

        createDemo(nextIp())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value(SOLD_OUT));

        assertThat(userRepository.count()).isEqualTo(users);
    }

    @Test
    void expiredDemosDoNotCountTowardsTheCap() throws Exception {
        fillActiveDemosUpTo(99);
        for (int i = 0; i < 5; i++) {
            fillers.add(insertDemoUser(Instant.now().minus(Duration.ofMinutes(5))));
        }

        String token = tokenOf(createDemo(nextIp()).andExpect(status().isCreated()));

        purgeDemoOf(token);
    }

    @Test
    void twoSimultaneousCreationsAtNinetyNineActiveCreateOneAndRefuseTheOther() throws Exception {
        fillActiveDemosUpTo(99);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<MvcResult>> calls = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                String ip = nextIp();
                calls.add(pool.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    return createDemo(ip).andReturn();
                }));
            }
            start.countDown();
            List<MvcResult> results = new ArrayList<>();
            for (Future<MvcResult> call : calls) {
                results.add(call.get(60, TimeUnit.SECONDS));
            }

            assertThat(results).extracting(result -> result.getResponse().getStatus())
                    .containsExactlyInAnyOrder(201, 503);
            MvcResult refused = results.stream()
                    .filter(result -> result.getResponse().getStatus() == 503).findFirst().orElseThrow();
            assertThat((String) JsonPath.read(refused.getResponse().getContentAsString(), "$.error"))
                    .isEqualTo("SERVICE_UNAVAILABLE");
            assertThat(userRepository.countByDemoExpiresAtAfter(Instant.now())).isEqualTo(100L);
            MvcResult created = results.stream()
                    .filter(result -> result.getResponse().getStatus() == 201).findFirst().orElseThrow();
            purgeDemoOf(JsonPath.read(created.getResponse().getContentAsString(), "$.token"));
        } finally {
            pool.shutdownNow();
        }
    }

    private record Scenario(int transactions, int settlements, int bankLines, BigDecimal grossTotal,
                            int runItems, int matched, int divergent, int discrepancies) {
    }

    private Scenario scenarioOf(String token) throws Exception {
        String base = "/api/merchants/" + onlyMerchantOf(token);
        String transactions = read(token, base + "/transactions?size=1000").andReturn().getResponse()
                .getContentAsString();
        List<Number> amounts = JsonPath.read(transactions, "$.content[*].amount");
        BigDecimal gross = amounts.stream()
                .map(amount -> new BigDecimal(amount.toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String run = read(token, base + "/reconciliations").andReturn().getResponse().getContentAsString();
        String runId = JsonPath.read(run, "$.content[0].id");
        String divergent = read(token, base + "/reconciliations/" + runId + "/items?result=DIVERGENT&size=1000")
                .andReturn().getResponse().getContentAsString();
        List<Object> discrepancies = JsonPath.read(divergent, "$.content[*].discrepancies[*]");
        return new Scenario(
                JsonPath.read(transactions, "$.totalElements"),
                totalOf(token, base + "/external-settlements"),
                totalOf(token, base + "/bank-statements"),
                gross,
                JsonPath.read(run, "$.content[0].totalItems"),
                JsonPath.read(run, "$.content[0].matchedCount"),
                JsonPath.read(run, "$.content[0].divergentCount"),
                discrepancies.size());
    }

    private int totalOf(String token, String path) throws Exception {
        return JsonPath.read(read(token, path).andReturn().getResponse().getContentAsString(), "$.totalElements");
    }

    private String onlyMerchantOf(String token) throws Exception {
        String merchants = read(token, "/api/merchants")
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(merchants, "$.content[0].id");
    }

    private ResultActions read(String token, String path) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private ResultActions createDemo(String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/demo").with(request -> {
            request.setRemoteAddr(ip);
            return request;
        }));
    }

    private static String tokenOf(ResultActions created) throws Exception {
        return JsonPath.read(created.andReturn().getResponse().getContentAsString(), "$.token");
    }

    private static String nextIp() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return "10.%d.%d.%d".formatted(random.nextInt(256), random.nextInt(256), random.nextInt(1, 255));
    }

    // The suite shares one database, so the cap is reached by topping up whatever is already active.
    private void fillActiveDemosUpTo(int active) {
        long missing = active - userRepository.countByDemoExpiresAtAfter(Instant.now());
        assertThat(missing).isNotNegative();
        for (long i = 0; i < missing; i++) {
            fillers.add(insertDemoUser(Instant.now().plus(Duration.ofHours(1))));
        }
        assertThat(userRepository.countByDemoExpiresAtAfter(Instant.now())).isEqualTo(active);
    }

    private UUID insertDemoUser(Instant demoExpiresAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, name, email, password, role, active, created_at, updated_at, demo_expires_at)
                VALUES (?, 'Visitante', ?, 'not-used', 'OPERATOR', true, now(), now(), ?)
                """, id, "demo-filler-" + id + "@demo.reconpay.invalid", Timestamp.from(demoExpiresAt));
        return id;
    }

    // Keeps the shared database below the cap for the classes that run after this one.
    private void purgeDemoOf(String token) throws Exception {
        String me = read(token, "/api/me").andReturn().getResponse().getContentAsString();
        purger.purge(UUID.fromString(JsonPath.read(me, "$.id")));
    }
}
