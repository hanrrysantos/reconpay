package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.auth.repository.IUserMerchantAccessRepository;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationDiscrepancyRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationItemRepository;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DiscrepancyResolutionIntegrationTest extends AbstractIntegrationTest {

    private static final String ACCEPT = """
            {"status":"ACCEPTED","note":"ok"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IReconciliationDiscrepancyRepository discrepancyRepository;

    @Autowired
    private IReconciliationItemRepository itemRepository;

    @Autowired
    private IUserMerchantAccessRepository userMerchantAccessRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private static final UUID ADMIN_ID = UUID.fromString("a0000000-0000-4000-8000-000000000101");

    private String adminToken;
    private String operatorToken;
    private String merchantId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
        operatorToken = IntegrationTestUtils.obtainOperatorToken(mockMvc);
        merchantId = createMerchant("Merchant Resolution");
        IntegrationTestUtils.grantOperatorAccess(mockMvc, adminToken, UUID.fromString(merchantId));
        createFeeRule();
    }

    private void createFeeRule() throws Exception {
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
    void patchAndGetReturnTheSameAcceptedStatusAndOneTransition() throws Exception {
        OpenDiscrepancy open = openDiscrepancy();

        String patched = mockMvc.perform(patch(path(open), open.discrepancyId())
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCEPT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(open.discrepancyId().toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.transitions.length()").value(1))
                .andExpect(jsonPath("$.transitions[0].fromStatus").value("OPEN"))
                .andExpect(jsonPath("$.transitions[0].toStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.transitions[0].note").value("ok"))
                .andExpect(jsonPath("$.transitions[0].createdAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String fetched = mockMvc.perform(get(path(open), open.discrepancyId())
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(fetched).isEqualTo(patched);
        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}/items", merchantId, open.runId())
                        .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].expectedNetAmount").value(96.50))
                .andExpect(jsonPath("$.content[0].transactionStatus").value("APPROVED"));
    }

    @Test
    void unauthenticatedPatchIsUnauthorizedAndLeavesTheDiscrepancyOpen() throws Exception {
        OpenDiscrepancy open = openDiscrepancy();

        mockMvc.perform(patch(path(open), open.discrepancyId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCEPT))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(path(open), open.discrepancyId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.transitions.length()").value(0));
    }

    @Test
    void operatorWithoutGrantIsForbiddenAndLeavesTheDiscrepancyOpen() throws Exception {
        merchantId = createMerchant("Merchant Sem Grant");
        createFeeRule();
        OpenDiscrepancy open = openDiscrepancy();

        mockMvc.perform(patch(path(open), open.discrepancyId())
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCEPT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        mockMvc.perform(get(path(open), open.discrepancyId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void discrepancyOnAnotherRunIsNotFound() throws Exception {
        OpenDiscrepancy open = openDiscrepancy();

        mockMvc.perform(patch("/api/merchants/{merchantId}/reconciliations/{runId}/discrepancies/{discrepancyId}",
                        merchantId, UUID.randomUUID(), open.discrepancyId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCEPT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void supersededRunRejectsTheChangeAndKeepsTheDiscrepancyOpen() throws Exception {
        OpenDiscrepancy open = openDiscrepancy();
        String secondRunId = startRun();
        awaitCompleted(secondRunId);

        mockMvc.perform(patch(path(open), open.discrepancyId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCEPT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));

        mockMvc.perform(get(path(open), open.discrepancyId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.transitions.length()").value(0));
    }

    @Test
    void concurrentAcceptPersistsOneAndConflictsTheOther() throws Exception {
        OpenDiscrepancy open = openDiscrepancy();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<MvcResult> accept = () -> {
            start.await(5, TimeUnit.SECONDS);
            return mockMvc.perform(patch(path(open), open.discrepancyId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ACCEPT))
                    .andReturn();
        };
        try {
            Future<MvcResult> first = pool.submit(accept);
            Future<MvcResult> second = pool.submit(accept);
            start.countDown();
            List<MvcResult> results = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertThat(results).extracting(result -> result.getResponse().getStatus())
                    .containsExactlyInAnyOrder(200, 409);
            String conflict = results.stream()
                    .filter(result -> result.getResponse().getStatus() == 409)
                    .findFirst()
                    .orElseThrow()
                    .getResponse()
                    .getContentAsString();
            assertThat(conflict).contains("CONFLICT");
        } finally {
            pool.shutdownNow();
        }

        mockMvc.perform(get(path(open), open.discrepancyId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.transitions.length()").value(1));
    }

    @Test
    void invalidNoteAndAmountReturnValidationErrorAndLeaveTheDiscrepancyOpen() throws Exception {
        OpenDiscrepancy open = openDiscrepancy();
        String[] bodies = {
                "{\"status\":\"ACCEPTED\",\"note\":\"" + "x".repeat(501) + "\"}",
                "{\"status\":\"ADJUSTED\"}",
                "{\"status\":\"ADJUSTED\",\"correctionAmount\":0}",
                "{\"status\":\"ADJUSTED\",\"correctionAmount\":1.001}",
                "{\"status\":\"ADJUSTED\",\"correctionAmount\":100000000000000000.00}",
                "{\"status\":\"ACCEPTED\",\"correctionAmount\":1.00}",
                "{\"status\":\"WRITTEN_OFF\",\"correctionAmount\":1.00}",
                "{\"status\":\"OPEN\",\"correctionAmount\":1.00}"
        };
        for (String body : bodies) {
            mockMvc.perform(patch(path(open), open.discrepancyId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }
        mockMvc.perform(get(path(open), open.discrepancyId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.transitions.length()").value(0));
    }

    @Test
    void writtenOffAndAdjustedThenReopenKeepTheSaleAndTheVoidedCorrection() throws Exception {
        OpenDiscrepancy writtenOff = openDiscrepancy();
        mockMvc.perform(patch(path(writtenOff), writtenOff.discrepancyId())
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"WRITTEN_OFF\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WRITTEN_OFF"))
                .andExpect(jsonPath("$.transitions.length()").value(1))
                .andExpect(jsonPath("$.adjustments.length()").value(0));
        mockMvc.perform(patch(path(writtenOff), writtenOff.discrepancyId())
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.transitions.length()").value(2))
                .andExpect(jsonPath("$.adjustments.length()").value(0));

        OpenDiscrepancy adjusted = openDiscrepancy();
        mockMvc.perform(patch(path(adjusted), adjusted.discrepancyId())
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ADJUSTED\",\"correctionAmount\":-1.50,\"note\":\"fee\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ADJUSTED"))
                .andExpect(jsonPath("$.adjustments.length()").value(1))
                .andExpect(jsonPath("$.adjustments[0].amount").value(-1.50))
                .andExpect(jsonPath("$.adjustments[0].voided").value(false))
                .andExpect(jsonPath("$.transitions.length()").value(1))
                .andExpect(jsonPath("$.transitions[0].note").value("fee"));
        mockMvc.perform(patch(path(adjusted), adjusted.discrepancyId())
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.adjustments.length()").value(1))
                .andExpect(jsonPath("$.adjustments[0].voided").value(true))
                .andExpect(jsonPath("$.adjustments[0].amount").value(-1.50))
                .andExpect(jsonPath("$.transitions.length()").value(2));
    }

    @Test
    void adminWithoutMerchantGrantCanAccept() throws Exception {
        OpenDiscrepancy open = openDiscrepancy();
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                userMerchantAccessRepository.deleteAllByUserId(ADMIN_ID));

        mockMvc.perform(patch(path(open), open.discrepancyId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCEPT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.transitions.length()").value(1));
    }

    private String path(OpenDiscrepancy open) {
        return "/api/merchants/" + merchantId + "/reconciliations/" + open.runId() + "/discrepancies/{discrepancyId}";
    }

    private OpenDiscrepancy openDiscrepancy() throws Exception {
        createTransaction("TXN-MISSING-" + UUID.randomUUID());
        String runId = startRun();
        awaitCompleted(runId);
        UUID runUuid = UUID.fromString(runId);
        var runItemIds = new HashSet<>(itemRepository.findIdsByRunId(runUuid, Pageable.unpaged()).getContent());
        UUID discrepancyId = discrepancyRepository.findAll().stream()
                .filter(discrepancy -> runItemIds.contains(discrepancy.getReconciliationItem().getId()))
                .map(ReconciliationDiscrepancyEntity::getId)
                .findFirst()
                .orElseThrow();
        return new OpenDiscrepancy(runId, discrepancyId);
    }

    private String startRun() throws Exception {
        String runResponse = mockMvc.perform(post("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromDate": "2026-07-01",
                                  "toDate": "2026-07-31"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(runResponse, "$.id");
    }

    private void awaitCompleted(String id) {
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, id)
                                .header("Authorization", "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("COMPLETED")));
    }

    private String createMerchant(String name) throws Exception {
        String document = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        String response = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","document":"%s"}
                                """.formatted(name, document)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private void createTransaction(String externalReference) throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalReference": "%s",
                                  "amount": 100.00,
                                  "paymentMethod": "CREDIT_CARD",
                                  "installments": 3,
                                  "transactionDate": "2026-07-29"
                                }
                                """.formatted(externalReference)))
                .andExpect(status().isCreated());
    }

    private record OpenDiscrepancy(String runId, UUID discrepancyId) {
    }
}
