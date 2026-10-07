package br.com.hanrry.reconpay.bankstatement.integration;

import br.com.hanrry.reconpay.bankstatement.repository.IBankStatementLineRepository;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockingDetails;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class BankStatementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private IBankStatementLineRepository bankStatementLineRepository;

    @LocalServerPort
    private int port;

    private String adminToken;
    private String operatorToken;
    private String merchantId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
        operatorToken = IntegrationTestUtils.obtainOperatorToken(mockMvc);
        merchantId = createMerchant("Merchant Bank Statement");
        IntegrationTestUtils.grantOperatorAccess(mockMvc, adminToken, UUID.fromString(merchantId));
    }

    @Test
    void shouldImportAndListOnlyThatMerchantsLines() throws Exception {
        String withCode = "LN-" + UUID.randomUUID();
        String withoutCode = "LN-" + UUID.randomUUID();

        String importResponse = mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", merchantId)
                        .file(csvFile("""
                                lineReference,externalReference,amount,movementDate
                                %s,TXN-001,150.50,2026-07-30
                                %s,,80.00,2026-07-29
                                """.formatted(withCode, withoutCode)))
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.fileName").value("statement.csv"))
                .andExpect(jsonPath("$.totalRows").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String importId = com.jayway.jsonpath.JsonPath.read(importResponse, "$.id");

        String otherMerchantId = createMerchant("Merchant Outro Extrato");
        String otherLine = "LN-OTHER-" + UUID.randomUUID();
        mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", otherMerchantId)
                        .file(csvFile("""
                                lineReference,externalReference,amount,movementDate
                                %s,TXN-OTHER,40.00,2026-07-15
                                """.formatted(otherLine)))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/merchants/{merchantId}/bank-statements", merchantId)
                        .header("Authorization", "Bearer " + operatorToken)
                        .param("importId", importId)
                        .param("sort", "movementDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").isNotEmpty())
                .andExpect(jsonPath("$.content[0].lineReference").value(withCode))
                .andExpect(jsonPath("$.content[0].externalReference").value("TXN-001"))
                .andExpect(jsonPath("$.content[0].amount").value(150.50))
                .andExpect(jsonPath("$.content[0].movementDate").value("2026-07-30"))
                .andExpect(jsonPath("$.content[0].importId").value(importId))
                .andExpect(jsonPath("$.content[1].lineReference").value(withoutCode))
                .andExpect(jsonPath("$.content[1].externalReference").value(nullValue()))
                .andExpect(jsonPath("$.content[1].amount").value(80.00))
                .andExpect(jsonPath("$.content[1].movementDate").value("2026-07-29"))
                .andExpect(jsonPath("$.content[1].importId").value(importId))
                .andExpect(jsonPath("$.content[?(@.lineReference=='" + otherLine + "')]").isEmpty());

        mockMvc.perform(get("/api/merchants/{merchantId}/bank-statements", otherMerchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].lineReference").value(otherLine))
                .andExpect(jsonPath("$.content[?(@.lineReference=='" + withCode + "')]").isEmpty());
    }

    @Test
    void shouldRejectInvalidRowAndPersistNothing() throws Exception {
        mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", merchantId)
                        .file(csvFile("""
                                lineReference,externalReference,amount,movementDate
                                ,TXN-001,150.00,2026-07-30
                                """))
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.rowErrors[0].row").value(2))
                .andExpect(jsonPath("$.details.rowErrors[0].message").value("Referência da linha é obrigatória"));

        assertNothingPersisted(merchantId);
    }

    @Test
    void shouldRejectDuplicateLineReferenceInFileAndPersistNothing() throws Exception {
        mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", merchantId)
                        .file(csvFile("""
                                lineReference,externalReference,amount,movementDate
                                LN-DUP,TXN-001,150.00,2026-07-30
                                LN-DUP,TXN-002,80.00,2026-07-29
                                """))
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.rowErrors[0].row").value(3))
                .andExpect(jsonPath("$.details.rowErrors[0].message")
                        .value("Referência da linha duplicada no arquivo: LN-DUP"));

        assertNothingPersisted(merchantId);
    }

    @Test
    void shouldRejectSecondImportOfSameLineReferenceAndKeepTheFirst() throws Exception {
        String lineReference = "LN-DUP-" + UUID.randomUUID();

        mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", merchantId)
                        .file(csvFile("""
                                lineReference,externalReference,amount,movementDate
                                %s,TXN-001,10.00,2026-07-30
                                """.formatted(lineReference)))
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isCreated());

        mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", merchantId)
                        .file(csvFile("""
                                lineReference,externalReference,amount,movementDate
                                %s,TXN-002,99.00,2026-07-28
                                """.formatted(lineReference)))
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.details.conflictingReferences[0]").value(lineReference));

        mockMvc.perform(get("/api/merchants/{merchantId}/bank-statements", merchantId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].lineReference").value(lineReference))
                .andExpect(jsonPath("$.content[0].externalReference").value("TXN-001"))
                .andExpect(jsonPath("$.content[0].amount").value(10.00))
                .andExpect(jsonPath("$.content[0].movementDate").value("2026-07-30"));

        assertThat(countImports(merchantId)).isEqualTo(1L);
        assertThat(countLines(merchantId)).isEqualTo(1L);
    }

    @Test
    void shouldRejectInvalidHeaderAndPersistNothing() throws Exception {
        mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", merchantId)
                        .file(csvFile("""
                                ref,codigo,valor,data
                                LN-001,TXN-001,150.00,2026-07-30
                                """))
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(
                        "Cabeçalho CSV inválido. Esperado: lineReference,externalReference,amount,movementDate"));

        assertNothingPersisted(merchantId);
    }

    @Test
    void shouldRejectMissingFileAndNonCsvAndPersistNothing() throws Exception {
        mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", merchantId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "statement.csv",
                "text/csv",
                new byte[0]);
        mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", merchantId)
                        .file(emptyFile)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Arquivo CSV é obrigatório"));

        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "statement.txt",
                "text/plain",
                "content".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", merchantId)
                        .file(textFile)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Arquivo deve ser um CSV (.csv)"));

        assertNothingPersisted(merchantId);
    }

    @Test
    void shouldRejectOversizedCsvAndPersistNothing() throws Exception {
        HttpResponse<String> response = IntegrationTestUtils.postMultipartFile(
                port,
                "/api/merchants/" + merchantId + "/bank-statements/import",
                operatorToken,
                "statement.csv",
                IntegrationTestUtils.csvLargerThanFiveMegabytes());

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(com.jayway.jsonpath.JsonPath.read(response.body(), "$.error").toString())
                .isEqualTo("VALIDATION_ERROR");
        assertThat(com.jayway.jsonpath.JsonPath.read(response.body(), "$.message").toString())
                .isEqualTo("Arquivo CSV excede o tamanho máximo permitido de 5MB");

        assertNothingPersisted(merchantId);
    }

    @Test
    void shouldPersistOneWhenTwoImportsOfTheSameNewLineReferenceRunTogether() throws Exception {
        String lineReference = "LN-RACE-" + UUID.randomUUID();
        byte[] csv = """
                lineReference,externalReference,amount,movementDate
                %s,TXN-RACE,10.00,2026-07-30
                """.formatted(lineReference).getBytes(StandardCharsets.UTF_8);

        CountDownLatch bothPassedCheck = new CountDownLatch(2);
        Answer<?> delegate = mockingDetails(bankStatementLineRepository)
                .getMockCreationSettings()
                .getDefaultAnswer();
        doAnswer(invocation -> {
            Object found = delegate.answer(invocation);
            if (found instanceof List<?> rows && !rows.isEmpty()) {
                throw new IllegalStateException(
                        "duplicate check saw a committed row; the imports were not simultaneous");
            }
            bothPassedCheck.countDown();
            if (!bothPassedCheck.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("the other import did not pass the duplicate check");
            }
            return found;
        }).when(bankStatementLineRepository).findByMerchant_IdAndLineReferenceIn(any(), any());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<MvcResult> importCsv = () -> {
            start.await(5, TimeUnit.SECONDS);
            return mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", merchantId)
                            .file(new MockMultipartFile("file", "statement.csv", "text/csv", csv))
                            .header("Authorization", "Bearer " + operatorToken))
                    .andReturn();
        };
        List<MvcResult> results;
        try {
            Future<MvcResult> first = pool.submit(importCsv);
            Future<MvcResult> second = pool.submit(importCsv);
            start.countDown();
            results = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        assertThat(results)
                .extracting(result -> result.getResponse().getStatus())
                .containsExactlyInAnyOrder(201, 409);
        String conflict = results.stream()
                .filter(result -> result.getResponse().getStatus() == 409)
                .findFirst()
                .orElseThrow()
                .getResponse()
                .getContentAsString();
        assertThat(com.jayway.jsonpath.JsonPath.read(conflict, "$.error").toString()).isEqualTo("CONFLICT");
        assertThat(com.jayway.jsonpath.JsonPath.read(conflict, "$.message").toString())
                .isEqualTo("Conflito com um registro existente. Tente novamente.");

        mockMvc.perform(get("/api/merchants/{merchantId}/bank-statements", merchantId)
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].lineReference").value(lineReference));
        assertThat(countImports(merchantId)).isEqualTo(1L);
        assertThat(countLines(merchantId)).isEqualTo(1L);
    }

    @Test
    void shouldRejectUnauthenticatedImport() throws Exception {
        mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", merchantId)
                        .file(csvFile("""
                                lineReference,externalReference,amount,movementDate
                                LN-ANON,TXN-001,150.00,2026-07-30
                                """)))
                .andExpect(status().isUnauthorized());

        assertNothingPersisted(merchantId);
    }

    @Test
    void operatorWithoutGrantShouldReceiveForbiddenAndPersistNothing() throws Exception {
        String ungrantedMerchantId = createMerchant("Merchant Sem Grant");

        mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", ungrantedMerchantId)
                        .file(csvFile("""
                                lineReference,externalReference,amount,movementDate
                                LN-NO-GRANT,TXN-001,150.00,2026-07-30
                                """))
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        assertNothingPersisted(ungrantedMerchantId);
    }

    @Test
    void adminWithoutGrantShouldImportBankStatement() throws Exception {
        String adminLookup = mockMvc.perform(get("/api/users/email")
                        .param("email", "admin@reconpay.local")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String adminId = com.jayway.jsonpath.JsonPath.read(adminLookup, "$.id");

        mockMvc.perform(put("/api/users/{id}/merchants", adminId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"merchantIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantIds").isEmpty());

        String lineReference = "LN-ADMIN-" + UUID.randomUUID();
        mockMvc.perform(multipart("/api/merchants/{merchantId}/bank-statements/import", merchantId)
                        .file(csvFile("""
                                lineReference,externalReference,amount,movementDate
                                %s,TXN-ADMIN,150.00,2026-07-30
                                """.formatted(lineReference)))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("statement.csv"))
                .andExpect(jsonPath("$.totalRows").value(1));

        mockMvc.perform(get("/api/merchants/{merchantId}/bank-statements", merchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lineReference").value(lineReference))
                .andExpect(jsonPath("$.content[0].externalReference").value("TXN-ADMIN"));
    }

    private String createMerchant(String name) throws Exception {
        String uniqueDocument = UUID.randomUUID().toString().replace("-", "").substring(0, 14);

        String merchantResponse = mockMvc.perform(post("/api/merchants")
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
        mockMvc.perform(get("/api/merchants/{merchantId}/bank-statements", targetMerchantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        assertThat(countImports(targetMerchantId)).isZero();
        assertThat(countLines(targetMerchantId)).isZero();
    }

    private long countImports(String targetMerchantId) {
        Long count = jdbc.queryForObject(
                "select count(*) from bank_statement_imports where merchant_id = ?",
                Long.class,
                UUID.fromString(targetMerchantId));
        return count == null ? 0 : count;
    }

    private long countLines(String targetMerchantId) {
        Long count = jdbc.queryForObject(
                "select count(*) from bank_statement_lines where merchant_id = ?",
                Long.class,
                UUID.fromString(targetMerchantId));
        return count == null ? 0 : count;
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file",
                "statement.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
