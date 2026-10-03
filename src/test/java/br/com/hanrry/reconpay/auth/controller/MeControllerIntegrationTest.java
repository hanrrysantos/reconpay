package br.com.hanrry.reconpay.auth.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MeControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;
    private String operatorToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
        operatorToken = IntegrationTestUtils.obtainOperatorToken(mockMvc);
    }

    @Test
    void shouldReturnCurrentUserForOperator() throws Exception {
        mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("analyst@reconpay.local"))
                .andExpect(jsonPath("$.role").value("OPERATOR"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void operatorShouldSeeOnlyGrantedMerchants() throws Exception {
        String document = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        String merchantResponse = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Loja Me Test",
                                  "document": "%s"
                                }
                                """.formatted(document)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID merchantId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(merchantResponse, "$.id"));
        IntegrationTestUtils.grantOperatorAccess(mockMvc, adminToken, merchantId);

        mockMvc.perform(get("/api/me/merchants")
                        .header("Authorization", "Bearer " + operatorToken)
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + merchantId + "')]").exists());
    }

    @Test
    void unauthenticatedMeShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }
}
