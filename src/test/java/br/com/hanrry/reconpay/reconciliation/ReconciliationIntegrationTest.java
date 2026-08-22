package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ReconciliationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;
    private String analystToken;
    private String merchantId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
        analystToken = IntegrationTestUtils.obtainAnalystToken(mockMvc);

        String uniqueDocument = UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        String merchantResponse = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "Merchant Reconciliation",
                                  "document": "%s"
                                }
                                """, uniqueDocument)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        merchantId = com.jayway.jsonpath.JsonPath.read(merchantResponse, "$.id");

        IntegrationTestUtils.grantAnalystAccess(mockMvc, adminToken, UUID.fromString(merchantId));

        mockMvc.perform(post("/api/merchants/{merchantId}/fee-rules", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CREDIT_CARD",
                                  "installments": 3,
                                  "feePercentage": 3.0000,
                                  "fixedFee": 0.50
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRunReconciliationListItemsAndExportCsv() throws Exception {
        String matchedReference = "TXN-MATCH-" + UUID.randomUUID();
        String missingReference = "TXN-MISSING-" + UUID.randomUUID();
        String orphanReference = "EXT-ORPHAN-" + UUID.randomUUID();
        String feeDivergenceReference = "TXN-FEE-" + UUID.randomUUID();

        createTransaction(matchedReference, "150.00", "CREDIT_CARD", 3);
        createTransaction(missingReference, "100.00", "CREDIT_CARD", 3);
        createTransaction(feeDivergenceReference, "150.00", "CREDIT_CARD", 3);

        importSettlements("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                %s,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                %s,150.00,140.00,CREDIT_CARD,3,APPROVED,2026-07-30
                %s,90.00,88.00,PIX,1,APPROVED,2026-07-30
                """.formatted(matchedReference, feeDivergenceReference, orphanReference));

        String runResponse = mockMvc.perform(post("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromDate": "2026-07-01",
                                  "toDate": "2026-07-31"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location", containsString("/reconciliations/")))
                .andExpect(jsonPath("$.merchantId").value(merchantId))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String runId = com.jayway.jsonpath.JsonPath.read(runResponse, "$.id");

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, runId)
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.finishedAt").isNotEmpty())
                .andExpect(jsonPath("$.totalItems").value(4))
                .andExpect(jsonPath("$.matchedCount").value(1))
                .andExpect(jsonPath("$.divergentCount").value(3));

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}/items", merchantId, runId)
                        .header("Authorization", "Bearer " + analystToken)
                        .param("result", "DIVERGENT")
                        .param("discrepancyType", "MISSING_SETTLEMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalReference").value(missingReference))
                .andExpect(jsonPath("$.content[0].discrepancies[0].type").value("MISSING_SETTLEMENT"));

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}/export", merchantId, runId)
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("reconciliation-" + runId + ".csv")))
                .andExpect(content().string(containsString("externalReference")))
                .andExpect(content().string(containsString(matchedReference)))
                .andExpect(content().string(containsString("MATCHED")))
                .andExpect(content().string(containsString("FEE_DIVERGENCE")));
    }

    @Test
    void analystShouldNotRunReconciliation() throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pastRunShouldKeepReportingWhatItCompared() throws Exception {
        String reference = "TXN-SNAPSHOT-" + UUID.randomUUID();
        String transactionId = createTransaction(reference, "150.00", "CREDIT_CARD", 3);

        importSettlements("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                %s,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """.formatted(reference));

        String runId = runReconciliation("2026-07-01", "2026-07-31");

        mockMvc.perform(patch("/api/merchants/{merchantId}/transactions/{id}/status", merchantId, transactionId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CHARGEBACK"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}/items", merchantId, runId)
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionStatus").value("APPROVED"))
                .andExpect(jsonPath("$.content[0].result").value("MATCHED"));
    }

    @Test
    void rerunShouldSupersedeThePreviousRunForTheSameWindow() throws Exception {
        String firstRunId = runReconciliation("2026-07-01", "2026-07-31");
        String secondRunId = runReconciliation("2026-07-01", "2026-07-31");

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, firstRunId)
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supersededAt").isNotEmpty());

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, secondRunId)
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supersededAt").doesNotExist());
    }

    @Test
    void settlementLandingAfterTheWindowShouldStillMatchWithinLag() throws Exception {
        String reference = "TXN-LAG-" + UUID.randomUUID();
        createTransaction(reference, "150.00", "CREDIT_CARD", 3, "2026-07-31");

        importSettlements("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                %s,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-08-02
                """.formatted(reference));

        String runId = runReconciliation("2026-07-01", "2026-07-31");

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}/items", merchantId, runId)
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalReference").value(reference))
                .andExpect(jsonPath("$.content[0].result").value("MATCHED"));
    }

    @Test
    void acceptedRunShouldReachCompletedWithoutFurtherRequests() throws Exception {
        String runId = runReconciliation("2026-06-01", "2026-06-30");

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, runId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.startedAt").isNotEmpty())
                .andExpect(jsonPath("$.finishedAt").isNotEmpty())
                .andExpect(jsonPath("$.errorMessage").doesNotExist());
    }

    @Test
    void shouldRejectRunWithoutDateWindow() throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectWindowWiderThanTheConfiguredLimit() throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromDate": "2020-01-01",
                                  "toDate": "2026-12-31"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    private String runReconciliation(String fromDate, String toDate) throws Exception {
        String response = mockMvc.perform(post("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromDate": "%s",
                                  "toDate": "%s"
                                }
                                """.formatted(fromDate, toDate)))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private String createTransaction(
            String externalReference,
            String amount,
            String paymentMethod,
            int installments) throws Exception {
        return createTransaction(externalReference, amount, paymentMethod, installments, "2026-07-29");
    }

    private String createTransaction(
            String externalReference,
            String amount,
            String paymentMethod,
            int installments,
            String transactionDate) throws Exception {
        String response = mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "externalReference": "%s",
                                  "amount": %s,
                                  "paymentMethod": "%s",
                                  "installments": %d,
                                  "transactionDate": "%s"
                                }
                                """, externalReference, amount, paymentMethod, installments, transactionDate)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private void importSettlements(String csvContent) throws Exception {
        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "settlements.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());
    }
}
