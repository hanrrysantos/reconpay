package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationItemRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Window 2026-06-01..06-03, current run (bank tracked, 3 items, 1 matched):
 *   S1  06-01 MATCHED    100.00  97.20  97.20  97.20
 *   S2  06-01 DIVERGENT   50.55  48.99  48.00  48.00  FEE_DIVERGENCE OPEN
 *   S3  06-03 DIVERGENT   10.10   9.81   null   null  MISSING_SETTLEMENT OPEN
 * A superseded run of the same window matched 3 of 4; the preceding window
 * 05-29..05-31 has a current run that sold 80.40.
 * The scheduler is parked so a seeded RUNNING run stays in flight.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "reconpay.reconciliation.dispatch-delay-ms=3600000")
class PeriodSummaryIntegrationTest extends AbstractIntegrationTest {

    private static final String FROM = "2026-06-01";
    private static final String TO = "2026-06-03";

    @Autowired MockMvc mockMvc;
    @Autowired IMerchantRepository merchants;
    @Autowired IReconciliationRunRepository runs;
    @Autowired IReconciliationItemRepository items;

    private String adminToken;
    private String operatorToken;
    private String merchantId;
    private UUID currentRunId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
        operatorToken = IntegrationTestUtils.obtainOperatorToken(mockMvc);
        merchantId = createMerchant("Merchant Summary");
        IntegrationTestUtils.grantOperatorAccess(mockMvc, adminToken, UUID.fromString(merchantId));
        MerchantEntity merchant = merchants.findById(UUID.fromString(merchantId)).orElseThrow();

        ReconciliationRunEntity superseded = run(merchant, FROM, TO, ReconciliationRunStatus.COMPLETED, 4, 3);
        superseded.setSupersededAt(Instant.parse("2026-06-04T10:00:00Z"));
        runs.saveAndFlush(superseded);

        ReconciliationRunEntity current = run(merchant, FROM, TO, ReconciliationRunStatus.COMPLETED, 3, 1);
        current.setBankTracked(true);
        current = runs.saveAndFlush(current);
        currentRunId = current.getId();
        item(current, "S1", "2026-06-01", ReconciliationResult.MATCHED, "100.00", "97.20", "97.20", "97.20", null, null);
        item(current, "S2", "2026-06-01", ReconciliationResult.DIVERGENT, "50.55", "48.99", "48.00", "48.00",
                DiscrepancyType.FEE_DIVERGENCE, DiscrepancyStatus.OPEN);
        item(current, "S3", "2026-06-03", ReconciliationResult.DIVERGENT, "10.10", "9.81", null, null,
                DiscrepancyType.MISSING_SETTLEMENT, DiscrepancyStatus.OPEN);

        ReconciliationRunEntity previousWindow = runs.saveAndFlush(
                run(merchant, "2026-05-29", "2026-05-31", ReconciliationRunStatus.COMPLETED, 1, 1));
        item(previousWindow, "W1", "2026-05-30", ReconciliationResult.MATCHED, "80.40", "78.00", "78.00", "78.00", null, null);

        runs.saveAndFlush(run(merchant, "2026-09-01", "2026-09-30", ReconciliationRunStatus.RUNNING, 0, 0));
    }

    @Test
    void operatorWithGrantAndAdminReadTheSameFullSummary() throws Exception {
        String operatorBody = summary(operatorToken, merchantId, FROM, TO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(currentRunId.toString()))
                .andExpect(jsonPath("$.fromDate").value(FROM))
                .andExpect(jsonPath("$.toDate").value(TO))
                .andExpect(jsonPath("$.openByType.FEE_DIVERGENCE").value(1))
                .andExpect(jsonPath("$.openByType.MISSING_SETTLEMENT").value(1))
                .andExpect(jsonPath("$.openByType.length()").value(2))
                .andExpect(jsonPath("$.resolvedCount").value(0))
                .andExpect(jsonPath("$.days.length()").value(3))
                .andExpect(jsonPath("$.days[0].date").value("2026-06-01"))
                .andExpect(jsonPath("$.days[0].itemCount").value(2))
                .andExpect(jsonPath("$.days[0].matchedCount").value(1))
                .andExpect(jsonPath("$.days[1].date").value("2026-06-02"))
                .andExpect(jsonPath("$.days[1].itemCount").value(0))
                .andExpect(jsonPath("$.days[1].matchedCount").value(0))
                .andExpect(jsonPath("$.days[2].date").value("2026-06-03"))
                .andExpect(jsonPath("$.days[2].itemCount").value(1))
                .andExpect(jsonPath("$.days[2].matchedCount").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(operatorBody)
                .contains("\"grossAmount\":160.65")
                .contains("\"expectedNetAmount\":156.00")
                .contains("\"feeAmount\":4.65")
                .contains("\"feeAboveContractAmount\":0.99")
                .contains("\"bankReceivedAmount\":145.20")
                .contains("\"previousRunMatchRate\":0.7500")
                .contains("\"previousWindowGrossAmount\":80.40")
                .contains("\"feePercent\":2.89")
                .contains("\"previousWindowGrossChangePercent\":99.8")
                .contains("{\"date\":\"2026-06-01\",\"expectedNetAmount\":146.19,\"settledNetAmount\":145.20,"
                        + "\"bankReceivedAmount\":145.20,\"bankShortfallAmount\":0.99,\"itemCount\":2,\"matchedCount\":1}")
                .contains("{\"date\":\"2026-06-02\",\"expectedNetAmount\":0.00,\"settledNetAmount\":0.00,"
                        + "\"bankReceivedAmount\":0.00,\"bankShortfallAmount\":0.00,\"itemCount\":0,\"matchedCount\":0}")
                .contains("{\"date\":\"2026-06-03\",\"expectedNetAmount\":9.81,\"settledNetAmount\":0.00,"
                        + "\"bankReceivedAmount\":0.00,\"bankShortfallAmount\":9.81,\"itemCount\":1,\"matchedCount\":0}");

        String adminBody = summary(adminToken, merchantId, FROM, TO)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(adminBody).isEqualTo(operatorBody);
    }

    @Test
    void orphanSettlementCreditStaysOutOfTheBankTotalSoItEqualsTheSumOfTheDays() throws Exception {
        ReconciliationRunEntity current = runs.findById(currentRunId).orElseThrow();
        item(current, "O1", null, ReconciliationResult.DIVERGENT, null, null, "30.00", "30.00",
                DiscrepancyType.ORPHAN_SETTLEMENT, DiscrepancyStatus.OPEN);

        String body = summary(adminToken, merchantId, FROM, TO)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        BigDecimal daysTotal = JsonPath.<java.util.List<Object>>read(body, "$.days[*].bankReceivedAmount").stream()
                .map(value -> new BigDecimal(value.toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = new BigDecimal(JsonPath.read(body, "$.bankReceivedAmount").toString());
        assertThat(daysTotal).isEqualByComparingTo("145.20");
        assertThat(total).isEqualByComparingTo(daysTotal);
        assertThat(body)
                .contains("\"grossAmount\":160.65")
                .contains("\"expectedNetAmount\":156.00")
                .contains("\"feeAmount\":4.65")
                .contains("\"feeAboveContractAmount\":0.99");
    }

    @Test
    void invalidWindowReturnsValidationError() throws Exception {
        summary(adminToken, merchantId, TO, FROM)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        summary(operatorToken, merchantId, "2020-01-01", "2026-12-31")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/merchants/{merchantId}/periods/summary", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("fromDate", FROM))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void missingTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/periods/summary", merchantId)
                        .param("fromDate", FROM)
                        .param("toDate", TO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void operatorWithoutGrantIsForbidden() throws Exception {
        String otherMerchant = createMerchant("Merchant Summary Sem Grant");

        summary(operatorToken, otherMerchant, FROM, TO)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void windowWithoutCurrentCompletedRunReturnsNotFound() throws Exception {
        summary(operatorToken, merchantId, "2026-08-01", "2026-08-31")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void windowWithRunInFlightReturnsConflict() throws Exception {
        summary(operatorToken, merchantId, "2026-09-01", "2026-09-30")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    private ResultActions summary(String token, String targetMerchantId, String fromDate, String toDate)
            throws Exception {
        return mockMvc.perform(get("/api/merchants/{merchantId}/periods/summary", targetMerchantId)
                .header("Authorization", "Bearer " + token)
                .param("fromDate", fromDate)
                .param("toDate", toDate));
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

    private static ReconciliationRunEntity run(
            MerchantEntity merchant,
            String fromDate,
            String toDate,
            ReconciliationRunStatus status,
            int total,
            int matched) {
        ReconciliationRunEntity run = new ReconciliationRunEntity();
        run.setMerchant(merchant);
        run.setFromDate(LocalDate.parse(fromDate));
        run.setToDate(LocalDate.parse(toDate));
        run.setStatus(status);
        run.setTotalItems(total);
        run.setMatchedCount(matched);
        run.setDivergentCount(total - matched);
        return run;
    }

    private void item(
            ReconciliationRunEntity run,
            String reference,
            String saleDate,
            ReconciliationResult result,
            String transaction,
            String expectedNet,
            String settledNet,
            String bank,
            DiscrepancyType type,
            DiscrepancyStatus status) {
        ReconciliationItemEntity item = new ReconciliationItemEntity();
        item.setReconciliationRun(run);
        item.setExternalReference(reference);
        item.setResult(result);
        item.setTransactionDate(saleDate == null ? null : LocalDate.parse(saleDate));
        item.setTransactionAmount(money(transaction));
        item.setExpectedNetAmount(money(expectedNet));
        item.setSettlementNetAmount(money(settledNet));
        item.setBankAmount(money(bank));
        if (type != null) {
            ReconciliationDiscrepancyEntity discrepancy = new ReconciliationDiscrepancyEntity();
            discrepancy.setType(type);
            discrepancy.setStatus(status);
            item.addDiscrepancy(discrepancy);
        }
        items.saveAndFlush(item);
    }

    private static BigDecimal money(String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
