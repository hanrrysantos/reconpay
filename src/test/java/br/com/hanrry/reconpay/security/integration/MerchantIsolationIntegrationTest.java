package br.com.hanrry.reconpay.security.integration;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MerchantIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;
    private String operatorToken;
    private UUID grantedMerchantId;
    private UUID otherMerchantId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
        operatorToken = IntegrationTestUtils.obtainOperatorToken(mockMvc);

        grantedMerchantId = createMerchant("Merchant Concedido");
        otherMerchantId = createMerchant("Merchant Alheio");

        IntegrationTestUtils.grantOperatorAccess(mockMvc, adminToken, grantedMerchantId);
    }

    @Test
    void analystShouldReadTransactionsOfGrantedMerchant() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", grantedMerchantId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk());
    }

    @Test
    void analystShouldNotReadTransactionsOfAnotherMerchant() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", otherMerchantId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void analystShouldNotReadReconciliationsOfAnotherMerchant() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations", otherMerchantId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void analystShouldNotReadSettlementsOfAnotherMerchant() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements", otherMerchantId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void revokingAccessShouldRemoveVisibility() throws Exception {
        UUID operatorId = IntegrationTestUtils.operatorId(mockMvc, adminToken);

        mockMvc.perform(put("/api/users/{id}/merchants", operatorId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantIds").isEmpty());

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", grantedMerchantId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminShouldReachEveryMerchantWithoutAGrant() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", otherMerchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void analystShouldNotManageMerchantAccess() throws Exception {
        UUID operatorId = IntegrationTestUtils.operatorId(mockMvc, adminToken);

        mockMvc.perform(put("/api/users/{id}/merchants", operatorId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantIds\":[]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void grantingAccessToAnUnknownMerchantShouldFail() throws Exception {
        UUID operatorId = IntegrationTestUtils.operatorId(mockMvc, adminToken);

        mockMvc.perform(put("/api/users/{id}/merchants", operatorId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantIds\":[\"%s\"]}".formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    private UUID createMerchant(String name) throws Exception {
        String document = UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        String response = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "document": "%s"
                                }
                                """.formatted(name, document)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(response, "$.id"));
    }
}
