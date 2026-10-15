package br.com.hanrry.reconpay.merchant.integration;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import br.com.hanrry.reconpay.util.TestCnpj;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

@AutoConfigureMockMvc
class MerchantIntegrationTest extends AbstractIntegrationTest {

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
    void shouldCreateListUpdateAndDeleteMerchant() throws Exception {
        String document = TestCnpj.random();
        String createResponse = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Loja Exemplo",
                                  "document": "%s"
                                }
                                """.formatted(document)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Loja Exemplo"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String merchantId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        // The suite shares one database and never cleans up, so the page has to be
        // wide enough to still contain this merchant after every other test ran.
        mockMvc.perform(get("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.document=='" + document + "')]").exists());

        mockMvc.perform(put("/api/merchants/{id}", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Loja Exemplo Atualizada"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Loja Exemplo Atualizada"));

        mockMvc.perform(delete("/api/merchants/{id}", merchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectDuplicateDocument() throws Exception {
        mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Loja A",
                                  "document": "11111111000191"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Loja B",
                                  "document": "11111111000191"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567800019", "12345678000199", "11111111111111", "00000000000000", "12A45678000195"})
    void shouldRejectInvalidCnpj(String document) throws Exception {
        mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Loja Inválida",
                                  "document": "%s"
                                }
                                """.formatted(document)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("CNPJ inválido"));
    }

    @Test
    void shouldReportOnlyRequiredMessageForBlankDocument() throws Exception {
        mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Loja Sem Documento",
                                  "document": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Documento é obrigatório"));
    }

    @Test
    void operatorShouldCreateMerchantAndSeeItInScopedList() throws Exception {
        String document = TestCnpj.random();
        String createResponse = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Loja Operator",
                                  "document": "%s"
                                }
                                """.formatted(document)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.document").value(document))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String merchantId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");

        mockMvc.perform(get("/api/merchants")
                        .header("Authorization", "Bearer " + operatorToken)
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + merchantId + "')]").exists());

        mockMvc.perform(put("/api/merchants/{id}", merchantId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Loja Operator Atualizada"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Loja Operator Atualizada"));
    }
}
