package br.com.hanrry.reconpay.transaction.integration;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.transaction.repository.IInternalTransactionRepository;
import br.com.hanrry.reconpay.transaction.repository.ITransactionImportRepository;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import br.com.hanrry.reconpay.util.TestCnpj;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Each merchant gets Pix 1x (1%, 0.00) and Crédito 3x (2.99%, 0.30), so every expected net below
 * can be checked by hand: 100.00 in Crédito 3x → 96.71; 50.00 in Pix → 49.50.
 */
@AutoConfigureMockMvc
class TransactionImportIntegrationTest extends AbstractIntegrationTest {

    private static final String HEADER = "externalReference,amount,paymentMethod,installments,status,transactionDate";

    @Autowired MockMvc mockMvc;
    @Autowired IInternalTransactionRepository transactions;
    @Autowired ITransactionImportRepository imports;
    @LocalServerPort int port;

    private String adminToken;
    private String merchantId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
        merchantId = createMerchantWithRules("Import merchant");
    }

    @Test
    void importsTheFileAndAnswersTheBatch() throws Exception {
        String body = importCsv(merchantId, "vendas-julho.csv",
                HEADER,
                "PED-1,100.00,CREDIT_CARD,3,APPROVED,2026-07-10",
                " PED-2 ,50.00,PIX,1,CANCELLED,2026-07-11")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchantId").value(merchantId))
                .andExpect(jsonPath("$.fileName").value("vendas-julho.csv"))
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getContentAsString();
        String importId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("sort", "externalReference,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].externalReference").value(contains("PED-1", "PED-2")))
                .andExpect(jsonPath("$.content[*].amount").value(contains(100.00, 50.00)))
                .andExpect(jsonPath("$.content[*].expectedNetAmount").value(contains(96.71, 49.50)))
                .andExpect(jsonPath("$.content[*].paymentMethod").value(contains("CREDIT_CARD", "PIX")))
                .andExpect(jsonPath("$.content[*].installments").value(contains(3, 1)))
                .andExpect(jsonPath("$.content[*].status").value(contains("APPROVED", "CANCELLED")))
                .andExpect(jsonPath("$.content[*].transactionDate").value(contains("2026-07-10", "2026-07-11")));

        assertThat(transactions.findAll()).filteredOn(t -> t.getMerchant().getId().toString().equals(merchantId))
                .allSatisfy(t -> assertThat(t.getImportBatch().getId().toString()).isEqualTo(importId));
    }

    @Test
    void theExpectedNetIsTheSameAsTheSingleCreate() throws Exception {
        importCsv(merchantId, "v.csv", HEADER, "CSV-1,123.45,CREDIT_CARD,3,APPROVED,2026-07-10").andExpect(status().isCreated());
        mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalReference": "FORM-1", "amount": 123.45, "paymentMethod": "CREDIT_CARD",
                                 "installments": 3, "transactionDate": "2026-07-10"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.content[*].expectedNetAmount").value(contains(119.46, 119.46)));
    }

    @Test
    void answersEveryRowErrorAtOnceAndSavesNothing() throws Exception {
        importCsv(merchantId, "v.csv",
                HEADER,
                "OK-1,10.00,PIX,1,APPROVED,2026-07-10",
                "BAD-1,0,PIX,1,APPROVED,2026-07-10",
                "BAD-2,10.00,BOLETO,1,APPROVED,2026-07-10",
                "BAD-3,10.00,PIX,2,PAID,2026-07-10")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Erro na importação do CSV"))
                .andExpect(jsonPath("$.details.rowErrors[*].row").value(contains(3, 4, 5, 5)))
                .andExpect(jsonPath("$.details.rowErrors[*].message").value(contains(
                        "amount deve ser maior que zero",
                        "Regra de taxa ativa não encontrada para BOLETO em 1x",
                        "Método de pagamento PIX não permite parcelamento",
                        "Status inválido")));

        assertNothingSaved(merchantId);
    }

    @Test
    void rejectsAWrongHeader() throws Exception {
        importCsv(merchantId, "v.csv", "externalReference,amount,paymentMethod,installments,transactionDate",
                "PED-1,10.00,PIX,1,2026-07-10")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Cabeçalho CSV inválido. Esperado: " + HEADER));
        assertNothingSaved(merchantId);
    }

    @Test
    void rejectsAFileWithOnlyTheHeader() throws Exception {
        importCsv(merchantId, "v.csv", HEADER, "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CSV não contém registros"));
        assertNothingSaved(merchantId);
    }

    @Test
    void rejectsAnEmptyFileAFileThatIsNotCsvAndARequestWithoutFile() throws Exception {
        mockMvc.perform(multipart("/api/merchants/{merchantId}/transactions/import", merchantId)
                        .file(new MockMultipartFile("file", "v.csv", "text/csv", new byte[0]))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Arquivo CSV é obrigatório"));

        importCsv(merchantId, "vendas.txt", HEADER, "PED-1,10.00,PIX,1,APPROVED,2026-07-10")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Arquivo deve ser um CSV (.csv)"));

        mockMvc.perform(multipart("/api/merchants/{merchantId}/transactions/import", merchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Requisição inválida. Verifique os campos e parâmetros enviados."));

        assertNothingSaved(merchantId);
    }

    @Test
    void rejectsAFileLargerThanFiveMegabytes() throws Exception {
        HttpResponse<String> response = IntegrationTestUtils.postMultipartFile(
                port,
                "/api/merchants/" + merchantId + "/transactions/import",
                adminToken,
                "vendas.csv",
                IntegrationTestUtils.csvLargerThanFiveMegabytes());

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(JsonPath.read(response.body(), "$.error").toString()).isEqualTo("VALIDATION_ERROR");
        assertThat(JsonPath.read(response.body(), "$.message").toString())
                .isEqualTo("Arquivo CSV excede o tamanho máximo permitido de 5MB");
        assertNothingSaved(merchantId);
    }

    @Test
    void sendingTheSameFileAgainAnswersEveryStoredReference() throws Exception {
        String[] file = {HEADER, "PED-1,10.00,PIX,1,APPROVED,2026-07-10", "PED-2,10.00,PIX,1,APPROVED,2026-07-10"};
        importCsv(merchantId, "v.csv", file).andExpect(status().isCreated());

        importCsv(merchantId, "v.csv", file)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Referências externas já cadastradas para este comerciante: PED-1, PED-2"))
                .andExpect(jsonPath("$.details.conflictingReferences").value(contains("PED-1", "PED-2")));

        assertThat(imports.findAllByMerchant_Id(UUID.fromString(merchantId), PageRequest.of(0, 20)).getTotalElements())
                .isEqualTo(1);
    }

    @Test
    void aLockedPeriodRejectsTheFileAndSavesNothing() throws Exception {
        completeEmptyWindow(merchantId, "2026-07-01", "2026-07-15");
        mockMvc.perform(post("/api/merchants/{merchantId}/periods/lock", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromDate\":\"2026-07-01\",\"toDate\":\"2026-07-15\"}"))
                .andExpect(status().isOk());

        importCsv(merchantId, "v.csv", HEADER,
                "OUT-1,10.00,PIX,1,APPROVED,2026-07-20",
                "IN-1,10.00,PIX,1,APPROVED,2026-07-10")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Janela travada de 2026-07-01 a 2026-07-15"));

        assertNothingSaved(merchantId);
    }

    @Test
    void theSameReferenceIsAcceptedForAnotherMerchant() throws Exception {
        String other = createMerchantWithRules("Other import merchant");
        importCsv(merchantId, "v.csv", HEADER, "PED-1,10.00,PIX,1,APPROVED,2026-07-10").andExpect(status().isCreated());

        importCsv(other, "v.csv", HEADER, "PED-1,10.00,PIX,1,APPROVED,2026-07-10").andExpect(status().isCreated());
    }

    @Test
    void listsTheBatchesNewestFirstAndFindsOneById() throws Exception {
        String first = JsonPath.read(importCsv(merchantId, "primeiro.csv", HEADER, "A-1,10.00,PIX,1,APPROVED,2026-07-10")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
        var created = importCsv(merchantId, "segundo.csv", HEADER,
                        "B-1,10.00,PIX,1,APPROVED,2026-07-10", "B-2,10.00,PIX,1,APPROVED,2026-07-10")
                .andExpect(status().isCreated())
                .andReturn().getResponse();
        String second = JsonPath.read(created.getContentAsString(), "$.id");
        assertThat(created.getHeader("Location")).endsWith("/api/merchants/" + merchantId + "/transactions/imports/" + second);

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions/imports", merchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(contains(second, first)))
                .andExpect(jsonPath("$.content[*].fileName").value(contains("segundo.csv", "primeiro.csv")))
                .andExpect(jsonPath("$.content[*].totalRows").value(contains(2, 1)))
                .andExpect(jsonPath("$.size").value(20));

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions/imports/{importId}", merchantId, first)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(first))
                .andExpect(jsonPath("$.merchantId").value(merchantId))
                .andExpect(jsonPath("$.fileName").value("primeiro.csv"))
                .andExpect(jsonPath("$.totalRows").value(1))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void aBatchOfAnotherMerchantIsNotFound() throws Exception {
        String other = createMerchantWithRules("Batch owner");
        String othersBatch = JsonPath.read(importCsv(other, "v.csv", HEADER, "X-1,10.00,PIX,1,APPROVED,2026-07-10")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/merchants/{merchantId}/transactions/imports/{importId}", merchantId, othersBatch)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Importação não encontrada com id: " + othersBatch));
    }

    @Test
    void anOperatorWithoutAccessIsForbiddenAndAnUnknownMerchantIsNotFound() throws Exception {
        String operatorToken = IntegrationTestUtils.obtainOperatorToken(mockMvc);
        mockMvc.perform(multipart("/api/merchants/{merchantId}/transactions/import", merchantId)
                        .file(file("v.csv", HEADER, "PED-1,10.00,PIX,1,APPROVED,2026-07-10"))
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/merchants/{merchantId}/transactions/imports", merchantId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden());

        UUID unknown = UUID.randomUUID();
        importCsv(unknown.toString(), "v.csv", HEADER, "PED-1,10.00,PIX,1,APPROVED,2026-07-10")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Comerciante não encontrado com id: " + unknown));
        mockMvc.perform(get("/api/merchants/{merchantId}/transactions/imports", unknown)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
        assertNothingSaved(merchantId);
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/merchants/{merchantId}/transactions/import", merchantId)
                        .file(file("v.csv", HEADER, "PED-1,10.00,PIX,1,APPROVED,2026-07-10")))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions importCsv(String targetMerchantId, String fileName, String... lines) throws Exception {
        return mockMvc.perform(multipart("/api/merchants/{merchantId}/transactions/import", targetMerchantId)
                .file(file(fileName, lines))
                .header("Authorization", "Bearer " + adminToken));
    }

    private static MockMultipartFile file(String fileName, String... lines) {
        return new MockMultipartFile("file", fileName, "text/csv", String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }

    private void assertNothingSaved(String targetMerchantId) {
        UUID id = UUID.fromString(targetMerchantId);
        assertThat(imports.findAllByMerchant_Id(id, PageRequest.of(0, 20)).getTotalElements()).isZero();
        assertThat(transactions.findAll()).noneMatch(t -> t.getMerchant().getId().equals(id));
    }

    private String createMerchantWithRules(String name) throws Exception {
        String id = JsonPath.read(mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\", \"document\": \"%s\"}".formatted(name, TestCnpj.random())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        for (String rule : List.of(
                "{\"paymentMethod\": \"PIX\", \"installments\": 1, \"feePercentage\": 1.0000, \"fixedFee\": 0.00}",
                "{\"paymentMethod\": \"CREDIT_CARD\", \"installments\": 3, \"feePercentage\": 2.9900, \"fixedFee\": 0.30}")) {
            mockMvc.perform(post("/api/merchants/{merchantId}/fee-rules", id)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(rule))
                    .andExpect(status().isCreated());
        }
        return id;
    }

    private void completeEmptyWindow(String targetMerchantId, String fromDate, String toDate) throws Exception {
        String runId = JsonPath.read(mockMvc.perform(post("/api/merchants/{merchantId}/reconciliations", targetMerchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromDate\":\"%s\",\"toDate\":\"%s\"}".formatted(fromDate, toDate)))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString(), "$.id");
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mockMvc.perform(MockMvcRequestBuilders.get("/api/merchants/{merchantId}/reconciliations/{runId}", targetMerchantId, runId)
                                .header("Authorization", "Bearer " + adminToken))
                        .andExpect(jsonPath("$.status").value("COMPLETED")));
    }
}
