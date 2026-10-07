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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ExternalSettlementIntegrationTest extends AbstractIntegrationTest {

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

        IntegrationTestUtils.grantOperatorAccess(mockMvc, adminToken, UUID.fromString(merchantId));
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
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(1));

        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements", merchantId)
                        .header("Authorization", "Bearer " + operatorToken)
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
                                .header("Authorization", "Bearer " + operatorToken))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.content[0].id");

        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements/{id}", merchantId, settlementId)
                        .header("Authorization", "Bearer " + operatorToken))
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

        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements/imports", merchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements", merchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalReference").value(externalReference));
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

        assertNothingPersisted(merchantId);
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

        assertNothingPersisted(merchantId);
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
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cabeçalho CSV inválido")));

        assertNothingPersisted(merchantId);
    }

    @Test
    void operatorShouldImportForGrantedMerchant() throws Exception {
        MockMultipartFile csvFile = csvFile("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                TXN-OP-IMPORT,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """);

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldImportReconpayLayoutWhenParameterIsPresent() throws Exception {
        String externalReference = "EXT-LAYOUT-" + UUID.randomUUID();

        MockMultipartFile csvFile = csvFile("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                %s,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """.formatted(externalReference));

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .param("layout", "RECONPAY")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRows").value(1));

        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements", merchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].externalReference").value(externalReference))
                .andExpect(jsonPath("$.content[0].netAmount").value(145.00));
    }

    @Test
    void shouldImportAcquirerLayoutAndReadMappedSettlement() throws Exception {
        String nsu = "NSU-" + UUID.randomUUID();

        MockMultipartFile csvFile = csvFile("""
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                %s,200.00,190.50,PIX,1,APPROVED,2026-07-29
                """.formatted(nsu));

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .param("layout", "ACQUIRER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRows").value(1));

        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements", merchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalReference").value(nsu))
                .andExpect(jsonPath("$.content[0].amount").value(200.00))
                .andExpect(jsonPath("$.content[0].netAmount").value(190.50))
                .andExpect(jsonPath("$.content[0].paymentMethod").value("PIX"))
                .andExpect(jsonPath("$.content[0].installments").value(1))
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.content[0].settlementDate").value("2026-07-29"));
    }

    @Test
    void shouldRejectUnknownLayoutAndPersistNothing() throws Exception {
        MockMultipartFile csvFile = csvFile("""
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                NSU-UNKNOWN,200.00,190.50,PIX,1,APPROVED,2026-07-29
                """);

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .param("layout", "CIELO")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        assertNothingPersisted(merchantId);
    }

    @Test
    void shouldRejectMismatchedAcquirerHeaderAndPersistNothing() throws Exception {
        MockMultipartFile csvFile = csvFile("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                TXN-SWAP,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """);

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .param("layout", "ACQUIRER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                        "nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao")));

        assertNothingPersisted(merchantId);
    }

    @Test
    void shouldRejectInvalidAcquirerRowAndPersistNothing() throws Exception {
        MockMultipartFile csvFile = csvFile("""
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                NSU-NET,100.00,150.00,PIX,1,APPROVED,2026-07-30
                """);

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .param("layout", "ACQUIRER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.rowErrors[0].message")
                        .value("netAmount não pode ser maior que amount"));

        assertNothingPersisted(merchantId);
    }

    @Test
    void shouldRejectDuplicateReferenceInsideAcquirerFileAndPersistNothing() throws Exception {
        MockMultipartFile csvFile = csvFile("""
                nsu,valor_bruto,valor_liquido,forma_pagamento,parcelas,situacao,data_liquidacao
                NSU-DUP,150.00,145.00,PIX,1,APPROVED,2026-07-30
                NSU-DUP,80.00,75.00,PIX,1,APPROVED,2026-07-30
                """);

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .param("layout", "ACQUIRER")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.rowErrors[0].row").value(3))
                .andExpect(jsonPath("$.details.rowErrors[0].message")
                        .value("Referência externa duplicada no arquivo: NSU-DUP"));

        assertNothingPersisted(merchantId);
    }

    @Test
    void shouldRejectMissingFileAndNonCsvAndPersistNothing() throws Exception {
        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "settlements.txt",
                "text/plain",
                "content".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(textFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Arquivo deve ser um CSV (.csv)"));

        assertNothingPersisted(merchantId);
    }

    @Test
    void shouldRejectUnauthenticatedImport() throws Exception {
        MockMultipartFile csvFile = csvFile("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                TXN-ANON,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """);

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile))
                .andExpect(status().isUnauthorized());

        assertNothingPersisted(merchantId);
    }

    @Test
    void operatorWithoutGrantShouldReceiveForbiddenAndPersistNothing() throws Exception {
        String ungrantedMerchantId = createMerchant("Merchant Sem Grant");

        MockMultipartFile csvFile = csvFile("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                TXN-NO-GRANT,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """);

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", ungrantedMerchantId)
                        .file(csvFile)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        assertNothingPersisted(ungrantedMerchantId);
    }

    @Test
    void adminWithoutGrantShouldImportSettlement() throws Exception {
        String adminLookup = mockMvc.perform(get("/api/users/email")
                        .param("email", "admin@reconpay.local")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String adminId = com.jayway.jsonpath.JsonPath.read(adminLookup, "$.id");

        String currentAccess = mockMvc.perform(get("/api/users/{id}/merchants", adminId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        java.util.List<String> merchantIds = new java.util.ArrayList<>(
                com.jayway.jsonpath.JsonPath.<java.util.List<String>>read(currentAccess, "$.merchantIds"));
        merchantIds.remove(merchantId);
        String remainingGrants = merchantIds.stream()
                .map("\"%s\""::formatted)
                .collect(java.util.stream.Collectors.joining(",", "{\"merchantIds\":[", "]}"));

        mockMvc.perform(put("/api/users/{id}/merchants", adminId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(remainingGrants))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantIds", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem(merchantId))));

        String externalReference = "EXT-ADMIN-" + UUID.randomUUID();
        MockMultipartFile csvFile = csvFile("""
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                %s,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """.formatted(externalReference));

        mockMvc.perform(multipart("/api/merchants/{merchantId}/external-settlements/import", merchantId)
                        .file(csvFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalRows").value(1));

        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements", merchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].externalReference").value(externalReference));
    }

    private String createMerchant(String name) throws Exception {
        String uniqueDocument = UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        String merchantResponse = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "document": "%s"
                                }
                                """.formatted(name, uniqueDocument)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(merchantResponse, "$.id");
    }

    private void assertNothingPersisted(String targetMerchantId) throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements", targetMerchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/merchants/{merchantId}/external-settlements/imports", targetMerchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file",
                "settlements.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
