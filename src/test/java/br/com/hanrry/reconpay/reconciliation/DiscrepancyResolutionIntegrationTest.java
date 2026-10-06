package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationDiscrepancyRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationItemRepository;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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

    private String adminToken;
    private String operatorToken;
    private String merchantId;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
        operatorToken = IntegrationTestUtils.obtainOperatorToken(mockMvc);
        merchantId = createMerchant("Merchant Resolution");
        IntegrationTestUtils.grantOperatorAccess(mockMvc, adminToken, UUID.fromString(merchantId));
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

        mockMvc.perform(patch(path(open), open.discrepancyId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCEPT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(open.discrepancyId().toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.transitions.length()").value(1))
                .andExpect(jsonPath("$.transitions[0].fromStatus").value("OPEN"))
                .andExpect(jsonPath("$.transitions[0].toStatus").value("ACCEPTED"));

        mockMvc.perform(get(path(open), open.discrepancyId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(open.discrepancyId().toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.transitions.length()").value(1))
                .andExpect(jsonPath("$.transitions[0].fromStatus").value("OPEN"))
                .andExpect(jsonPath("$.transitions[0].toStatus").value("ACCEPTED"));
    }

    @Test
    void unauthenticatedPatchIsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/merchants/{merchantId}/reconciliations/{runId}/discrepancies/{discrepancyId}",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCEPT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operatorWithoutGrantIsForbidden() throws Exception {
        String foreignMerchantId = createMerchant("Merchant Sem Grant");

        mockMvc.perform(patch("/api/merchants/{merchantId}/reconciliations/{runId}/discrepancies/{discrepancyId}",
                        foreignMerchantId, UUID.randomUUID(), UUID.randomUUID())
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ACCEPT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
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
        Callable<Integer> accept = () -> {
            start.await(5, TimeUnit.SECONDS);
            return mockMvc.perform(patch(path(open), open.discrepancyId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ACCEPT))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };
        try {
            Future<Integer> first = pool.submit(accept);
            Future<Integer> second = pool.submit(accept);
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }

        mockMvc.perform(get(path(open), open.discrepancyId())
                        .header("Authorization", "Bearer " + adminToken))
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
