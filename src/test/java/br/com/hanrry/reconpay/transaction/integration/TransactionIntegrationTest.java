package br.com.hanrry.reconpay.transaction.integration;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TransactionIntegrationTest extends AbstractIntegrationTest {

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
                                  "name": "Merchant Transactions",
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

        mockMvc.perform(post("/api/merchants/{merchantId}/fee-rules", merchantId)
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
                .andExpect(status().isCreated());
    }

    @Test
    void shouldCreateListFindAndUpdateTransactionStatus() throws Exception {
        String externalReference = "TXN-" + UUID.randomUUID();

        String createResponse = mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "externalReference": "%s",
                                  "amount": 150.00,
                                  "paymentMethod": "CREDIT_CARD",
                                  "installments": 3,
                                  "transactionDate": "2026-07-29"
                                }
                                """, externalReference)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantId").value(merchantId))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.expectedNetAmount").value(145.00))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String transactionId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + analystToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "APPROVED")
                        .param("paymentMethod", "CREDIT_CARD")
                        .param("fromDate", "2026-07-29")
                        .param("toDate", "2026-07-29"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].externalReference").value(externalReference));

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions/{id}", merchantId, transactionId)
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"));

        mockMvc.perform(patch("/api/merchants/{merchantId}/transactions/{id}/status", merchantId, transactionId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "REFUNDED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void shouldRejectDuplicateExternalReference() throws Exception {
        String externalReference = "TXN-DUP-" + UUID.randomUUID();
        String body = String.format("""
                {
                  "externalReference": "%s",
                  "amount": 100.00,
                  "paymentMethod": "PIX",
                  "installments": 1,
                  "transactionDate": "2026-07-29"
                }
                """, externalReference);

        mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void shouldRejectTransactionWithoutActiveFeeRule() throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "TXN-NO-FEE",
                                  "amount": 100.00,
                                  "paymentMethod": "BOLETO",
                                  "installments": 1,
                                  "transactionDate": "2026-07-29"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void shouldRejectPixWithMultipleInstallments() throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "TXN-PIX-3X",
                                  "amount": 100.00,
                                  "paymentMethod": "PIX",
                                  "installments": 3,
                                  "transactionDate": "2026-07-29"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectFutureTransactionDate() throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "TXN-FUTURE",
                                  "amount": 100.00,
                                  "paymentMethod": "PIX",
                                  "installments": 1,
                                  "transactionDate": "2099-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectInvalidStatusTransition() throws Exception {
        String externalReference = "TXN-STATUS-" + UUID.randomUUID();

        String createResponse = mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "externalReference": "%s",
                                  "amount": 100.00,
                                  "paymentMethod": "PIX",
                                  "installments": 1,
                                  "transactionDate": "2026-07-29"
                                }
                                """, externalReference)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String transactionId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(patch("/api/merchants/{merchantId}/transactions/{id}/status", merchantId, transactionId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "REFUNDED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/merchants/{merchantId}/transactions/{id}/status", merchantId, transactionId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "APPROVED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldFilterTransactionsByStatusPaymentMethodAndDateRange() throws Exception {
        createTransaction("TXN-FILTER-CC", "CREDIT_CARD", 3, "2026-07-28");
        createTransaction("TXN-FILTER-PIX-29", "PIX", 1, "2026-07-29");
        String refundedTransactionId = createTransaction("TXN-FILTER-PIX-30", "PIX", 1, "2026-07-30");

        mockMvc.perform(patch("/api/merchants/{merchantId}/transactions/{id}/status", merchantId, refundedTransactionId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "REFUNDED"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + analystToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + analystToken)
                        .param("status", "REFUNDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalReference").value("TXN-FILTER-PIX-30"));

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + analystToken)
                        .param("paymentMethod", "CREDIT_CARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalReference").value("TXN-FILTER-CC"));

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + analystToken)
                        .param("fromDate", "2026-07-29")
                        .param("toDate", "2026-07-29"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalReference").value("TXN-FILTER-PIX-29"));

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + analystToken)
                        .param("status", "APPROVED")
                        .param("paymentMethod", "PIX")
                        .param("fromDate", "2026-07-28")
                        .param("toDate", "2026-07-29"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalReference").value("TXN-FILTER-PIX-29"));

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + analystToken)
                        .param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    private String createTransaction(
            String externalReference,
            String paymentMethod,
            int installments,
            String transactionDate) throws Exception {
        String createResponse = mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "externalReference": "%s",
                                  "amount": 100.00,
                                  "paymentMethod": "%s",
                                  "installments": %d,
                                  "transactionDate": "%s"
                                }
                                """, externalReference, paymentMethod, installments, transactionDate)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");
    }

    @Test
    void financialAnalystShouldBeForbiddenOnCreateAndPatch() throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "TXN-FORBIDDEN",
                                  "amount": 100.00,
                                  "paymentMethod": "PIX",
                                  "installments": 1,
                                  "transactionDate": "2026-07-29"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/merchants/{merchantId}/transactions/{id}/status", merchantId, UUID.randomUUID())
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CANCELLED"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }
}
