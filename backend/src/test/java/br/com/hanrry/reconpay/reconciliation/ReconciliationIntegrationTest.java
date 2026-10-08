package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import com.jayway.jsonpath.JsonPath;
import com.opencsv.CSVReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ReconciliationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;
    private String operatorToken;
    private String merchantId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
        operatorToken = IntegrationTestUtils.obtainOperatorToken(mockMvc);

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

        IntegrationTestUtils.grantOperatorAccess(mockMvc, adminToken, UUID.fromString(merchantId));

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

        createTransaction("  " + matchedReference + "  ", "150.00", "CREDIT_CARD", 3);
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
        awaitCompleted(runId);

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, runId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.finishedAt").isNotEmpty())
                .andExpect(jsonPath("$.totalItems").value(4))
                .andExpect(jsonPath("$.matchedCount").value(0))
                .andExpect(jsonPath("$.divergentCount").value(4));

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}/items", merchantId, runId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .param("result", "DIVERGENT")
                        .param("discrepancyType", "MISSING_SETTLEMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalReference").value(missingReference))
                .andExpect(jsonPath("$.content[0].discrepancies[0].type").value("MISSING_SETTLEMENT"))
                .andExpect(jsonPath("$.content[0].discrepancies[0].id").isNotEmpty())
                .andExpect(jsonPath("$.content[0].discrepancies[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].discrepancies[0].adjustments").doesNotExist())
                .andExpect(jsonPath("$.content[0].discrepancies[0].transitions").doesNotExist())
                .andExpect(jsonPath("$.content[0].discrepancies.length()").value(1));

        String items = itemsJson(runId);
        assertThat(types(items, matchedReference)).containsExactly("MISSING_BANK_CREDIT");
        assertDiscrepancy(items, matchedReference, "MISSING_BANK_CREDIT", "145.00", null);
        assertDiscrepancy(items, feeDivergenceReference, "MISSING_BANK_CREDIT", "140.00", null);
        assertDiscrepancy(items, orphanReference, "MISSING_BANK_CREDIT", "88.00", null);
        assertThat(types(items, feeDivergenceReference))
                .containsExactlyInAnyOrder("FEE_DIVERGENCE", "MISSING_BANK_CREDIT");
        assertThat(types(items, orphanReference))
                .containsExactlyInAnyOrder("ORPHAN_SETTLEMENT", "MISSING_BANK_CREDIT");
        assertThat(types(items, missingReference)).containsExactly("MISSING_SETTLEMENT");

        String csv = mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}/export", merchantId, runId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("reconciliation-" + runId + ".csv")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertCsvHeader(csv);
        assertThat(csv).contains(matchedReference);
        assertThat(csv).contains("MISSING_BANK_CREDIT");
        assertThat(csv).contains("FEE_DIVERGENCE");
    }

    @Test
    void shouldPairStatementLinesOnTheCompletedRun() throws Exception {
        String exactReference = "TXN-EXACT-" + UUID.randomUUID();
        String mismatchReference = "TXN-MISMATCH-" + UUID.randomUUID();
        String missingBankReference = "TXN-NOBANK-" + UUID.randomUUID();
        String laggedReference = "TXN-LAGBANK-" + UUID.randomUUID();
        String outsideSale = "TXN-OLD-" + UUID.randomUUID();
        String orphanLine = "L-ORPHAN-" + UUID.randomUUID();
        String otherLine = "L-OTHER-" + UUID.randomUUID();
        String lateLine = "L-LATE-" + UUID.randomUUID();
        String ignoredLine = "L-OLD-" + UUID.randomUUID();

        createTransaction(exactReference, "150.00", "CREDIT_CARD", 3);
        createTransaction(mismatchReference, "150.00", "CREDIT_CARD", 3);
        createTransaction(missingBankReference, "150.00", "CREDIT_CARD", 3);
        createTransaction(laggedReference, "150.00", "CREDIT_CARD", 3, "2026-07-31");
        createTransaction(outsideSale, "150.00", "CREDIT_CARD", 3, "2026-06-15");

        importSettlements("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                %s,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                %s,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                %s,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                %s,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-08-03
                """.formatted(exactReference, mismatchReference, missingBankReference, laggedReference));

        importBankStatement("""
                lineReference,externalReference,amount,movementDate
                L-EXACT-%s,%s,145.00,2026-07-30
                L-MIS-%s,%s,100.00,2026-07-30
                %s,,20.00,2026-07-30
                L-LAG-%s,%s,145.00,2026-08-03
                %s,%s,145.00,2026-08-06
                %s,%s,145.00,2026-07-15
                """.formatted(
                UUID.randomUUID(), exactReference,
                UUID.randomUUID(), mismatchReference,
                orphanLine,
                UUID.randomUUID(), laggedReference,
                lateLine, missingBankReference,
                ignoredLine, outsideSale));

        String otherMerchantId = createMerchant("Merchant Outro Extrato");
        importBankStatement(otherMerchantId, """
                lineReference,externalReference,amount,movementDate
                %s,%s,145.00,2026-07-30
                """.formatted(otherLine, missingBankReference));

        String runId = runReconciliation("2026-07-01", "2026-07-31");

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, runId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalItems").value(5))
                .andExpect(jsonPath("$.matchedCount").value(2))
                .andExpect(jsonPath("$.divergentCount").value(3));

        String items = itemsJson(runId);
        assertThat(types(items, exactReference)).isEmpty();
        assertThat(resultOf(items, exactReference)).isEqualTo("MATCHED");
        assertDiscrepancy(items, mismatchReference, "BANK_AMOUNT_MISMATCH", "145.00", "100.00");
        assertThat(types(items, mismatchReference)).containsExactly("BANK_AMOUNT_MISMATCH");
        assertDiscrepancy(items, missingBankReference, "MISSING_BANK_CREDIT", "145.00", null);
        assertThat(types(items, missingBankReference)).containsExactly("MISSING_BANK_CREDIT");
        assertDiscrepancy(items, orphanLine, "ORPHAN_BANK_CREDIT", null, "20.00");
        assertThat(resultOf(items, orphanLine)).isEqualTo("DIVERGENT");
        assertThat(field(items, orphanLine, "internalTransactionId")).isNull();
        assertThat(field(items, orphanLine, "externalSettlementId")).isNull();
        assertThat(types(items, laggedReference)).isEmpty();
        assertThat(resultOf(items, laggedReference)).isEqualTo("MATCHED");
        assertThat(references(items)).doesNotContain(otherLine, lateLine, ignoredLine, outsideSale);

        String csv = mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}/export", merchantId, runId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String[]> rows = csvRows(csv);
        assertThat(rows.getFirst()).containsExactly(
                "externalReference",
                "result",
                "discrepancyTypes",
                "internalTransactionId",
                "externalSettlementId",
                "transactionAmount",
                "expectedNetAmount",
                "settlementAmount",
                "settlementNetAmount",
                "paymentMethod",
                "installments",
                "transactionStatus",
                "settlementStatus",
                "transactionDate",
                "settlementDate");
        assertThat(row(rows, mismatchReference)[2]).isEqualTo("BANK_AMOUNT_MISMATCH");
        assertThat(row(rows, missingBankReference)[2]).isEqualTo("MISSING_BANK_CREDIT");
        assertThat(row(rows, orphanLine)[2]).isEqualTo("ORPHAN_BANK_CREDIT");
        assertThat(row(rows, exactReference)[1]).isEqualTo("MATCHED");
        assertThat(row(rows, exactReference)[2]).isEmpty();
    }

    @Test
    void operatorShouldRunReconciliationForGrantedMerchant() throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromDate": "2026-07-01",
                                  "toDate": "2026-07-31"
                                }
                                """))
                .andExpect(status().isAccepted());
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
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionStatus").value("APPROVED"))
                .andExpect(jsonPath("$.content[0].result").value("DIVERGENT"))
                .andExpect(jsonPath("$.content[0].discrepancies.length()").value(1))
                .andExpect(jsonPath("$.content[0].discrepancies[0].type").value("MISSING_BANK_CREDIT"))
                .andExpect(jsonPath("$.content[0].discrepancies[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].discrepancies[0].expectedValue").value("145.00"))
                .andExpect(jsonPath("$.content[0].discrepancies[0].actualValue").value(nullValue()));
    }

    @Test
    void rerunShouldSupersedeThePreviousRunForTheSameWindow() throws Exception {
        String firstRunId = runReconciliation("2026-07-01", "2026-07-31");
        String secondRunId = runReconciliation("2026-07-01", "2026-07-31");

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, firstRunId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supersededAt").isNotEmpty());

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, secondRunId)
                        .header("Authorization", "Bearer " + operatorToken))
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
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalReference").value(reference))
                .andExpect(jsonPath("$.content[0].settlementDate").value("2026-08-02"))
                .andExpect(jsonPath("$.content[0].result").value("DIVERGENT"))
                .andExpect(jsonPath("$.content[0].discrepancies.length()").value(1))
                .andExpect(jsonPath("$.content[0].discrepancies[0].type").value("MISSING_BANK_CREDIT"))
                .andExpect(jsonPath("$.content[0].discrepancies[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[0].discrepancies[0].expectedValue").value("145.00"))
                .andExpect(jsonPath("$.content[0].discrepancies[0].actualValue").value(nullValue()));
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

        String id = JsonPath.read(response, "$.id");
        awaitCompleted(id);
        return id;
    }

    private void awaitCompleted(String id) {
        org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(15)).untilAsserted(() ->
                mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, id)
                                .header("Authorization", "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("COMPLETED")));
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

        return JsonPath.read(response, "$.id");
    }

    private String itemsJson(String runId) throws Exception {
        return mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}/items", merchantId, runId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void assertDiscrepancy(
            String items,
            String reference,
            String type,
            String expectedValue,
            String actualValue) {
        Map<String, Object> discrepancy = discrepancies(items, reference).stream()
                .filter(candidate -> type.equals(candidate.get("type")))
                .findFirst()
                .orElseThrow();
        assertThat(discrepancy.get("expectedValue")).isEqualTo(expectedValue);
        assertThat(discrepancy.get("actualValue")).isEqualTo(actualValue);
        assertThat(discrepancy.get("status")).isEqualTo("OPEN");
        assertThat(resultOf(items, reference)).isEqualTo("DIVERGENT");
    }

    private List<String> types(String items, String reference) {
        return discrepancies(items, reference).stream()
                .map(discrepancy -> (String) discrepancy.get("type"))
                .toList();
    }

    private String resultOf(String items, String reference) {
        return (String) item(items, reference).get("result");
    }

    private Object field(String items, String reference, String name) {
        return item(items, reference).get(name);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> discrepancies(String items, String reference) {
        return (List<Map<String, Object>>) item(items, reference).get("discrepancies");
    }

    private Map<String, Object> item(String items, String reference) {
        List<Map<String, Object>> found = JsonPath.read(
                items, "$.content[?(@.externalReference=='" + reference + "')]");
        assertThat(found).hasSize(1);
        return found.getFirst();
    }

    private List<String> references(String items) {
        return JsonPath.read(items, "$.content[*].externalReference");
    }

    private void assertCsvHeader(String csv) throws Exception {
        assertThat(csvRows(csv).getFirst()).containsExactly(
                "externalReference",
                "result",
                "discrepancyTypes",
                "internalTransactionId",
                "externalSettlementId",
                "transactionAmount",
                "expectedNetAmount",
                "settlementAmount",
                "settlementNetAmount",
                "paymentMethod",
                "installments",
                "transactionStatus",
                "settlementStatus",
                "transactionDate",
                "settlementDate");
    }

    private List<String[]> csvRows(String csv) throws Exception {
        try (CSVReader reader = new CSVReader(new StringReader(csv))) {
            return reader.readAll();
        }
    }

    private String[] row(List<String[]> rows, String reference) {
        return rows.stream()
                .filter(cells -> reference.equals(cells[0]))
                .findFirst()
                .orElseThrow();
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

    private void importBankStatement(String csvContent) throws Exception {
        importBankStatement(merchantId, csvContent);
    }

    private void importBankStatement(String targetMerchantId, String csvContent) throws Exception {
        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "statement.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", targetMerchantId)
                        .file(csvFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());
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
