package br.com.hanrry.reconpay.externalsettlement.integration;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ExternalSettlementIntegrationTest extends AbstractIntegrationTest {

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

        String merchantResponse = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "name": "Merchant External Settlements",
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
    void shouldImportListAndFindExternalSettlements() throws Exception {
        String externalReference = "EXT-" + UUID.randomUUID();

        MockMultipartFile csvFile = csvFile("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                %s,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """.formatted(externalReference));

        String importResponse = mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantId").value(merchantId))
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.fileName").value("settlements.csv"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String importId = com.jayway.jsonpath.JsonPath.read(importResponse, "$.id");

        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements/imports/{importId}", merchantId, importId)
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(1));

        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements", merchantId)
                        .header("Authorization", "Bearer " + analystToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "APPROVED")
                        .param("paymentMethod", "CREDIT_CARD")
                        .param("fromDate", "2026-07-30")
                        .param("toDate", "2026-07-30")
                        .param("importId", importId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].externalReference").value(externalReference))
                .andExpect(jsonPath("$.content[0].netAmount").value(145.00))
                .andExpect(jsonPath("$.content[0].importId").value(importId));

        String settlementId = com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements", merchantId)
                                .header("Authorization", "Bearer " + analystToken))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.content[0].id");

        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements/{id}", merchantId, settlementId)
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"));
    }

    @Test
    void shouldRejectDuplicateExternalReferenceOnImport() throws Exception {
        String externalReference = "EXT-DUP-" + UUID.randomUUID();

        MockMultipartFile firstImport = csvFile("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                %s,100.00,98.00,PIX,1,APPROVED,2026-07-30
                """.formatted(externalReference));

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(firstImport)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        MockMultipartFile secondImport = csvFile("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                %s,100.00,98.00,PIX,1,APPROVED,2026-07-30
                """.formatted(externalReference));

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(secondImport)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.details.conflictingReferences[0]").value(externalReference));
    }

    @Test
    void shouldRejectNetAmountGreaterThanAmount() throws Exception {
        MockMultipartFile csvFile = csvFile("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                TXN-NET,100.00,150.00,PIX,1,APPROVED,2026-07-30
                """);

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.rowErrors[0].message")
                        .value("netAmount não pode ser maior que amount"));
    }

    @Test
    void shouldRejectInvalidCsvRows() throws Exception {
        MockMultipartFile csvFile = csvFile("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                ,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                TXN-2,0.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """);

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.rowErrors.length()").value(2));
    }

    @Test
    void shouldRejectInvalidHeader() throws Exception {
        MockMultipartFile csvFile = csvFile("""
                ref,valor,liquido,metodo,parcelas,status,data
                TXN-1,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """);

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cabeçalho CSV inválido")));
    }

    @Test
    void analystShouldBeForbiddenOnImport() throws Exception {
        MockMultipartFile csvFile = csvFile("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                TXN-FORBIDDEN,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """);

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file",
                "settlements.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
