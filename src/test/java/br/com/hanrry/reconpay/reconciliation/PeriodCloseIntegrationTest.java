package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PeriodCloseIntegrationTest extends AbstractIntegrationTest {

    private static final String FROM = "2026-07-01";
    private static final String TO = "2026-07-15";
    private static final String WINDOW = """
            {"fromDate":"2026-07-01","toDate":"2026-07-15"}
            """;

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
    }

    @Test
    void lockedWindowRejectsAnInsideSaleAndAcceptsAnOutsideSaleAndTheNextDaySettlement() throws Exception {
        String merchantId = createMerchant("Merchant Fechamento");
        createPixFeeRule(merchantId);
        completeWindow(merchantId, FROM, TO);
        lock(merchantId, FROM, TO);

        mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transaction("TXN-INSIDE", "2026-07-10")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));

        mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transaction("TXN-OUTSIDE", "2026-07-20")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionDate").value("2026-07-20"));

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(settlementCsv("EXT-NEXT", "2026-07-16"))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRows").value(1));

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalReference").value("TXN-OUTSIDE"))
                .andExpect(jsonPath("$.content[0].transactionDate").value("2026-07-20"));

        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements", merchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].settlementDate").value("2026-07-16"));
    }

    @Test
    void differentReconciliationWindowIsAcceptedWhileTheExactWindowStaysLocked() throws Exception {
        String merchantId = createMerchant("Merchant Outra Janela");
        completeWindow(merchantId, FROM, TO);
        lock(merchantId, FROM, TO);

        mockMvc.perform(post("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromDate":"2026-08-01","toDate":"2026-08-15"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.fromDate").value("2026-08-01"))
                .andExpect(jsonPath("$.toDate").value("2026-08-15"));

        mockMvc.perform(post("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WINDOW))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));

        readPeriod(merchantId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.fromDate").value(FROM))
                .andExpect(jsonPath("$.toDate").value(TO));
    }

    @Test
    void feeRuleAndMerchantUpdateSucceedWhileTheWindowIsLocked() throws Exception {
        String merchantId = createMerchant("Merchant Livre");
        String feeRuleId = createPixFeeRule(merchantId);
        completeWindow(merchantId, FROM, TO);
        lock(merchantId, FROM, TO);

        mockMvc.perform(put("/api/merchants/{merchantId}/fee-rules/{id}", merchantId, feeRuleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feePercentage":2.5000,"fixedFee":0.10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feePercentage").value(2.5000))
                .andExpect(jsonPath("$.fixedFee").value(0.10));

        mockMvc.perform(put("/api/merchants/{id}", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Merchant Atualizado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Merchant Atualizado"));

        readPeriod(merchantId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(true));
    }

    @Test
    void twoConcurrentLocksPersistOneAndConflictTheOther() throws Exception {
        String merchantId = createMerchant("Merchant Corrida Trava");
        completeWindow(merchantId, FROM, TO);

        List<MvcResult> results = together(
                () -> postLock(merchantId),
                () -> postLock(merchantId));

        assertThat(results).extracting(result -> result.getResponse().getStatus())
                .containsExactlyInAnyOrder(200, 409);
        String conflict = bodyWithStatus(results, 409);
        assertThat(conflict).contains("CONFLICT");

        String lockedAt = JsonPath.read(bodyWithStatus(results, 200), "$.lockedAt");
        String period = readPeriod(merchantId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.openAmount").value(0.00))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(Instant.parse(JsonPath.read(period, "$.lockedAt")).truncatedTo(ChronoUnit.MICROS))
                .isEqualTo(Instant.parse(lockedAt).truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void twoConcurrentUnlocksPersistOneAndConflictTheOther() throws Exception {
        String merchantId = createMerchant("Merchant Corrida Reabertura");
        completeWindow(merchantId, FROM, TO);
        lock(merchantId, FROM, TO);

        List<MvcResult> results = together(
                () -> postUnlock(merchantId),
                () -> postUnlock(merchantId));

        assertThat(results).extracting(result -> result.getResponse().getStatus())
                .containsExactlyInAnyOrder(200, 409);
        assertThat(bodyWithStatus(results, 409)).contains("CONFLICT");
        assertThat(JsonPath.<Boolean>read(bodyWithStatus(results, 200), "$.locked")).isFalse();

        readPeriod(merchantId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.lockedAt").value(nullValue()));
    }

    @Test
    void lockAndSameWindowRunCommitExactlyOneAndConflictTheOther() throws Exception {
        String merchantId = createMerchant("Merchant Corrida Run");
        completeWindow(merchantId, FROM, TO);

        List<MvcResult> results = together(
                () -> postLock(merchantId),
                () -> postRun(merchantId, FROM, TO));

        List<Integer> statuses = results.stream()
                .map(result -> result.getResponse().getStatus())
                .toList();
        assertThat(statuses).contains(409);
        assertThat(statuses.stream().filter(status -> status == 409).count()).isEqualTo(1);
        assertThat(bodyWithStatus(results, 409)).contains("CONFLICT");

        MvcResult lockResult = results.get(0);
        MvcResult runResult = results.get(1);
        if (lockResult.getResponse().getStatus() == 200) {
            assertThat(runResult.getResponse().getStatus()).isEqualTo(409);
            readPeriod(merchantId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locked").value(true));
        } else {
            assertThat(lockResult.getResponse().getStatus()).isEqualTo(409);
            assertThat(runResult.getResponse().getStatus()).isEqualTo(202);
            String runId = JsonPath.read(runResult.getResponse().getContentAsString(), "$.id");
            awaitCompleted(merchantId, runId);
            readPeriod(merchantId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locked").value(false));
        }
    }

    private List<MvcResult> together(Callable<MvcResult> left, Callable<MvcResult> right) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<MvcResult> first = pool.submit(() -> {
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("race did not start");
                }
                return left.call();
            });
            Future<MvcResult> second = pool.submit(() -> {
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("race did not start");
                }
                return right.call();
            });
            start.countDown();
            return List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    private String bodyWithStatus(List<MvcResult> results, int status) throws Exception {
        return results.stream()
                .filter(result -> result.getResponse().getStatus() == status)
                .findFirst()
                .orElseThrow()
                .getResponse()
                .getContentAsString();
    }

    private MvcResult postLock(String merchantId) throws Exception {
        return mockMvc.perform(post("/api/merchants/{merchantId}/periods/lock", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WINDOW))
                .andReturn();
    }

    private MvcResult postUnlock(String merchantId) throws Exception {
        return mockMvc.perform(post("/api/merchants/{merchantId}/periods/unlock", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WINDOW))
                .andReturn();
    }

    private MvcResult postRun(String merchantId, String fromDate, String toDate) throws Exception {
        return mockMvc.perform(post("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromDate":"%s","toDate":"%s"}
                                """.formatted(fromDate, toDate)))
                .andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions readPeriod(String merchantId) throws Exception {
        return mockMvc.perform(get("/api/merchants/{merchantId}/periods", merchantId)
                .header("Authorization", "Bearer " + adminToken)
                .param("fromDate", FROM)
                .param("toDate", TO));
    }

    private void lock(String merchantId, String fromDate, String toDate) throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/periods/lock", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromDate":"%s","toDate":"%s"}
                                """.formatted(fromDate, toDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.openAmount").value(0.00));
    }

    private String createMerchant(String name) throws Exception {
        String document = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        String response = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","document":"%s"}
                                """.formatted(name, document)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createPixFeeRule(String merchantId) throws Exception {
        String response = mockMvc.perform(post("/api/merchants/{merchantId}/fee-rules", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "PIX",
                                  "installments": 1,
                                  "feePercentage": 1.0000,
                                  "fixedFee": 0.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String completeWindow(String merchantId, String fromDate, String toDate) throws Exception {
        MvcResult created = postRun(merchantId, fromDate, toDate);
        assertThat(created.getResponse().getStatus()).isEqualTo(202);
        String runId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        awaitCompleted(merchantId, runId);
        return runId;
    }

    private void awaitCompleted(String merchantId, String runId) {
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, runId)
                                .header("Authorization", "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("COMPLETED")));
    }

    private String transaction(String reference, String transactionDate) {
        return """
                {
                  "externalReference": "%s",
                  "amount": 100.00,
                  "paymentMethod": "PIX",
                  "installments": 1,
                  "transactionDate": "%s"
                }
                """.formatted(reference, transactionDate);
    }

    private MockMultipartFile settlementCsv(String reference, String settlementDate) {
        String csv = """
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                %s,100.00,98.00,PIX,1,APPROVED,%s
                """.formatted(reference, settlementDate);
        return new MockMultipartFile(
                "file",
                "settlements.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
    }
}
