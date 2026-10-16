package br.com.hanrry.reconpay.feerule.integration;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import br.com.hanrry.reconpay.util.TestCnpj;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

        String uniqueDocument = TestCnpj.random();

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

    @ParameterizedTest
    @ValueSource(strings = {"PIX", "BOLETO", "DEBIT_CARD"})
    void shouldRejectInstallmentsForSinglePaymentMethodsOnCreate(String paymentMethod) throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/fee-rules", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "paymentMethod": "%s",
                                  "installments": 2,
                                  "feePercentage": 1.0000,
                                  "fixedFee": 0.00
                                }
                                """, paymentMethod)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("Método de pagamento " + paymentMethod + " não permite parcelamento"));
    }

    @ParameterizedTest
    @CsvSource({"CREDIT_CARD, 12", "PIX, 1"})
    void shouldCreateFeeRuleForAllowedInstallments(String paymentMethod, int installments) throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/fee-rules", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "paymentMethod": "%s",
                                  "installments": %d,
                                  "feePercentage": 1.0000,
                                  "fixedFee": 0.00
                                }
                                """, paymentMethod, installments)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentMethod").value(paymentMethod))
                .andExpect(jsonPath("$.installments").value(installments));
    }

    @Test
    void shouldRejectUpdateToSinglePaymentMethodKeepingInstallmentsAndKeepRuleUnchanged() throws Exception {
        String feeRuleId = createFeeRule("CREDIT_CARD", 3);

        mockMvc.perform(put("/api/merchants/{merchantId}/fee-rules/{id}", merchantId, feeRuleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "PIX",
                                  "feePercentage": 1.5000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Método de pagamento PIX não permite parcelamento"));

        mockMvc.perform(get("/api/merchants/{merchantId}/fee-rules/{id}", merchantId, feeRuleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.installments").value(3))
                .andExpect(jsonPath("$.feePercentage").value(2.99));
    }

    @Test
    void shouldRejectUpdateAddingInstallmentsToPixRule() throws Exception {
        String feeRuleId = createFeeRule("PIX", 1);

        mockMvc.perform(put("/api/merchants/{merchantId}/fee-rules/{id}", merchantId, feeRuleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "installments": 2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Método de pagamento PIX não permite parcelamento"));

        mockMvc.perform(get("/api/merchants/{merchantId}/fee-rules/{id}", merchantId, feeRuleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.installments").value(1));
    }

    private String createFeeRule(String paymentMethod, int installments) throws Exception {
        String response = mockMvc.perform(post("/api/merchants/{merchantId}/fee-rules", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "paymentMethod": "%s",
                                  "installments": %d,
                                  "feePercentage": 2.9900,
                                  "fixedFee": 0.30
                                }
                                """, paymentMethod, installments)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }
}
