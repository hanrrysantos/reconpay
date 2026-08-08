package br.com.hanrry.reconpay;

import br.com.hanrry.reconpay.support.AbstractIntegrationTest;
import br.com.hanrry.reconpay.support.IntegrationTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TransactionSettlementLinkageIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;
    private String merchantId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);

        String uniqueDocument = UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        String merchantResponse = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "Merchant Linkage",
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
    void shouldAllowInternalTransactionAndExternalSettlementWithSameExternalReference() throws Exception {
        String externalReference = "TXN-LINK-" + UUID.randomUUID();

        mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
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
                .andExpect(jsonPath("$.externalReference").value(externalReference))
                .andExpect(jsonPath("$.expectedNetAmount").value(145.00));

        MockMultipartFile csvFile = new MockMultipartFile(
                "file",
                "settlements.csv",
                "text/csv",
                ("""
                        externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                        %s,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                        """.formatted(externalReference))
                        .getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRows").value(1));

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("paymentMethod", "CREDIT_CARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalReference").value(externalReference))
                .andExpect(jsonPath("$.content[0].amount").value(150.00))
                .andExpect(jsonPath("$.content[0].expectedNetAmount").value(145.00));

        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("paymentMethod", "CREDIT_CARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalReference").value(externalReference))
                .andExpect(jsonPath("$.content[0].amount").value(150.00))
                .andExpect(jsonPath("$.content[0].netAmount").value(145.00))
                .andExpect(jsonPath("$.content[0].paymentMethod").value("CREDIT_CARD"));
    }
}
