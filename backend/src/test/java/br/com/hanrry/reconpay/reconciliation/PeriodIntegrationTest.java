package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.auth.repository.IUserMerchantAccessRepository;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PeriodIntegrationTest extends AbstractIntegrationTest {

    private static final UUID ADMIN_ID = UUID.fromString("a0000000-0000-4000-8000-000000000101");
    private static final String FROM = "2026-07-01";
    private static final String TO = "2026-07-15";
    private static final String WINDOW = """
            {"fromDate":"2026-07-01","toDate":"2026-07-15"}
            """;
    private static final String INVERTED = """
            {"fromDate":"2026-07-15","toDate":"2026-07-01"}
            """;
    private static final String TOO_WIDE = """
            {"fromDate":"2020-01-01","toDate":"2026-12-31"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserMerchantAccessRepository userMerchantAccessRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String adminToken;
    private String operatorToken;
    private String merchantId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
        operatorToken = IntegrationTestUtils.obtainOperatorToken(mockMvc);
        merchantId = createMerchant("Merchant Period");
        IntegrationTestUtils.grantOperatorAccess(mockMvc, adminToken, UUID.fromString(merchantId));
    }

    @Test
    void operatorAndAdminWithoutMerchantRowReadTheSamePeriod() throws Exception {
        String runId = completeWindow();
        removeAdminMerchantRows();

        String operatorBody = readPeriod(operatorToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId))
                .andExpect(jsonPath("$.fromDate").value(FROM))
                .andExpect(jsonPath("$.toDate").value(TO))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.matchedCount").value(0))
                .andExpect(jsonPath("$.divergentCount").value(0))
                .andExpect(jsonPath("$.matchRate").value(nullValue()))
                .andExpect(jsonPath("$.openAmount").value(0.00))
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.lockedAt").value(nullValue()))
                .andExpect(jsonPath("$.closeDurationSeconds").value(nullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String adminBody = readPeriod(adminToken)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(adminBody).isEqualTo(operatorBody);
        assertThat(operatorBody).contains("\"openAmount\":0.00");
        assertThat(operatorBody).contains("\"matchRate\":null");
    }

    @Test
    void adminWithoutMerchantRowLocksAndOperatorUnlocksTheSameRun() throws Exception {
        String runId = completeWindow();
        String finishedAt = JsonPath.read(runJson(runId), "$.finishedAt");
        removeAdminMerchantRows();

        String lockedBody = mockMvc.perform(post("/api/merchants/{merchantId}/periods/lock", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WINDOW))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId))
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.matchRate").value(nullValue()))
                .andExpect(jsonPath("$.openAmount").value(0.00))
                .andExpect(jsonPath("$.lockedAt").isNotEmpty())
                .andExpect(jsonPath("$.closeDurationSeconds").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(lockedBody).contains("\"openAmount\":0.00");
        assertThat(lockedBody).contains("\"matchRate\":null");
        Instant lockedAt = Instant.parse(JsonPath.read(lockedBody, "$.lockedAt"));
        Number seconds = JsonPath.read(lockedBody, "$.closeDurationSeconds");
        assertThat(seconds.longValue())
                .isEqualTo(Duration.between(Instant.parse(finishedAt), lockedAt).toSeconds());

        mockMvc.perform(post("/api/merchants/{merchantId}/periods/unlock", merchantId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WINDOW))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId))
                .andExpect(jsonPath("$.matchRate").value(nullValue()))
                .andExpect(jsonPath("$.locked").value(false))
                .andExpect(jsonPath("$.lockedAt").value(nullValue()))
                .andExpect(jsonPath("$.closeDurationSeconds").value(nullValue()))
                .andExpect(jsonPath("$.openAmount").value(0.00));
    }

    @Test
    void unauthenticatedPeriodCallsReturn401AndLeaveTheWindowUnlocked() throws Exception {
        completeWindow();
        String before = readPeriod(adminToken).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/merchants/{merchantId}/periods", merchantId)
                        .param("fromDate", FROM)
                        .param("toDate", TO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        mockMvc.perform(post("/api/merchants/{merchantId}/periods/lock", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WINDOW))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        mockMvc.perform(post("/api/merchants/{merchantId}/periods/unlock", merchantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WINDOW))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        String after = readPeriod(adminToken).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(after).isEqualTo(before);
        assertThat(JsonPath.<Boolean>read(after, "$.locked")).isFalse();
    }

    @Test
    void operatorWithoutGrantIsForbiddenAndLeavesTheWindowUnlocked() throws Exception {
        merchantId = createMerchant("Merchant Sem Grant");
        completeWindow();

        readPeriod(operatorToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mockMvc.perform(post("/api/merchants/{merchantId}/periods/lock", merchantId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WINDOW))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mockMvc.perform(post("/api/merchants/{merchantId}/periods/unlock", merchantId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WINDOW))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        readPeriod(adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(false));
    }

    @Test
    void missingDatesReturnValidationError() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/periods", merchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/merchants/{merchantId}/periods", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("fromDate", FROM))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/merchants/{merchantId}/periods/lock", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/merchants/{merchantId}/periods/unlock", merchantId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromDate\":\"2026-07-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void invertedWindowReturnsValidationError() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/periods", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("fromDate", TO)
                        .param("toDate", FROM))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/merchants/{merchantId}/periods/lock", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVERTED))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/merchants/{merchantId}/periods/unlock", merchantId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVERTED))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void windowBeyondMaxDaysReturnsValidationError() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/periods", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("fromDate", "2020-01-01")
                        .param("toDate", "2026-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/merchants/{merchantId}/periods/lock", merchantId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TOO_WIDE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/merchants/{merchantId}/periods/unlock", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TOO_WIDE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void inactiveMerchantReturnsNotFound() throws Exception {
        String inactiveMerchantId = createMerchant("Merchant Inativo");
        completeWindow(inactiveMerchantId);
        readPeriod(adminToken, inactiveMerchantId).andExpect(status().isOk());

        mockMvc.perform(delete("/api/merchants/{id}", inactiveMerchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        readPeriod(adminToken, inactiveMerchantId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        mockMvc.perform(post("/api/merchants/{merchantId}/periods/lock", inactiveMerchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WINDOW))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        mockMvc.perform(post("/api/merchants/{merchantId}/periods/unlock", inactiveMerchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WINDOW))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    private ResultActions readPeriod(String token) throws Exception {
        return readPeriod(token, merchantId);
    }

    private ResultActions readPeriod(String token, String targetMerchantId) throws Exception {
        return mockMvc.perform(get("/api/merchants/{merchantId}/periods", targetMerchantId)
                .header("Authorization", "Bearer " + token)
                .param("fromDate", FROM)
                .param("toDate", TO));
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

    private String completeWindow() throws Exception {
        return completeWindow(merchantId);
    }

    private String completeWindow(String targetMerchantId) throws Exception {
        String response = mockMvc.perform(post("/api/merchants/{merchantId}/reconciliations", targetMerchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WINDOW))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String runId = JsonPath.read(response, "$.id");
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", targetMerchantId, runId)
                                .header("Authorization", "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("COMPLETED")));
        return runId;
    }

    private String runJson(String runId) throws Exception {
        return mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, runId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void removeAdminMerchantRows() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                userMerchantAccessRepository.deleteAllByUserId(ADMIN_ID));
    }
}
