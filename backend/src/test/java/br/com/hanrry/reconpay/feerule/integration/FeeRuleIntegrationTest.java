package br.com.hanrry.reconpay.feerule.integration;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

@AutoConfigureMockMvc
class FeeRuleIntegrationTest extends AbstractIntegrationTest {

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
                                  "name": "Merchant Fee Rule",
                                  "document": "%s"
                                }
                                """, uniqueDocument)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        merchantId = com.jayway.jsonpath.JsonPath.read(merchantResponse, "$.id");
    }

    @Test
    void shouldCreateListFindAndDeleteFeeRule() throws Exception {
        String createResponse = mockMvc.perform(post("/api/merchants/{merchantId}/fee-rules", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CREDIT_CARD",
                                  "installments": 1,
                                  "feePercentage": 3.0000,
                                  "fixedFee": 0.00
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantId").value(merchantId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String feeRuleId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(get("/api/merchants/{merchantId}/fee-rules", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].installments").value(1));

        mockMvc.perform(get("/api/merchants/{merchantId}/fee-rules/{id}", merchantId, feeRuleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"));

        mockMvc.perform(delete("/api/merchants/{merchantId}/fee-rules/{id}", merchantId, feeRuleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void operatorWithGrantShouldCreateAndListFeeRule() throws Exception {
        UUID merchantUuid = UUID.fromString(merchantId);
        IntegrationTestUtils.grantOperatorAccess(mockMvc, adminToken, merchantUuid);

        mockMvc.perform(post("/api/merchants/{merchantId}/fee-rules", merchantId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "DEBIT_CARD",
                                  "installments": 1,
                                  "feePercentage": 2.0000,
                                  "fixedFee": 0.50
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentMethod").value("DEBIT_CARD"));

        mockMvc.perform(get("/api/merchants/{merchantId}/fee-rules", merchantId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.paymentMethod=='DEBIT_CARD')]").exists());
    }

    @Test
    void shouldRejectDuplicateFeeRule() throws Exception {
        String corpo = """
                {
                  "paymentMethod": "PIX",
                  "installments": 1,
                  "feePercentage": 1.0000,
                  "fixedFee": 0.00
                }
                """;

        mockMvc.perform(post("/api/merchants/{merchantId}/fee-rules", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/merchants/{merchantId}/fee-rules", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }
}
