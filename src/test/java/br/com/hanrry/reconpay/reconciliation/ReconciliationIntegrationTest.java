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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantId").value(merchantId))
                .andExpect(jsonPath("$.totalItems").value(4))
                .andExpect(jsonPath("$.matchedCount").value(1))
                .andExpect(jsonPath("$.divergentCount").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String runId = com.jayway.jsonpath.JsonPath.read(runResponse, "$.id");

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, runId)
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(4));

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

    private void createTransaction(
            String externalReference,
            String amount,
            String paymentMethod,
            int installments) throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "externalReference": "%s",
                                  "amount": %s,
                                  "paymentMethod": "%s",
                                  "installments": %d,
                                  "transactionDate": "2026-07-29"
                                }
                                """, externalReference, amount, paymentMethod, installments)))
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
