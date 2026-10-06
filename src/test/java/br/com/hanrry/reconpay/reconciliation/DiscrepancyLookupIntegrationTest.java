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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DiscrepancyLookupIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IReconciliationDiscrepancyRepository discrepancyRepository;

    @Autowired
    private IReconciliationItemRepository itemRepository;

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
                                  "name": "Merchant Discrepancy Lookup",
                                  "document": "%s"
                                }
                                """, uniqueDocument)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        merchantId = com.jayway.jsonpath.JsonPath.read(merchantResponse, "$.id");

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
    void findByIdAndRunAndMerchant_matchesScopeAndRejectsWrongMerchantOrRun() throws Exception {
        String missingReference = "TXN-MISSING-" + UUID.randomUUID();
        createTransaction(missingReference, "100.00", "CREDIT_CARD", 3);

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

        String runId = com.jayway.jsonpath.JsonPath.read(runResponse, "$.id");
        awaitCompleted(runId);

        UUID runUuid = UUID.fromString(runId);
        UUID merchantUuid = UUID.fromString(merchantId);
        var runItemIds = new HashSet<>(itemRepository.findIdsByRunId(runUuid, Pageable.unpaged()).getContent());

        UUID discrepancyId = discrepancyRepository.findAll().stream()
                .filter(d -> runItemIds.contains(d.getReconciliationItem().getId()))
                .map(ReconciliationDiscrepancyEntity::getId)
                .findFirst()
                .orElseThrow();

        assertThat(discrepancyRepository.findByIdAndRunAndMerchant(discrepancyId, runUuid, merchantUuid))
                .isPresent()
                .get()
                .extracting(ReconciliationDiscrepancyEntity::getId)
                .isEqualTo(discrepancyId);

        assertThat(discrepancyRepository.findByIdAndRunAndMerchant(discrepancyId, runUuid, UUID.randomUUID()))
                .isEmpty();

        assertThat(discrepancyRepository.findByIdAndRunAndMerchant(discrepancyId, UUID.randomUUID(), merchantUuid))
                .isEmpty();
    }

    private void awaitCompleted(String id) {
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, id)
                                .header("Authorization", "Bearer " + adminToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("COMPLETED")));
    }

    private void createTransaction(
            String externalReference,
            String amount,
            String paymentMethod,
            int installments) throws Exception {
        mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "externalReference": "%s",
                                  "amount": %s,
                                  "paymentMethod": "%s",
                                  "installments": %d,
                                  "transactionDate": "2026-07-29"
                                }
                                """, externalReference, amount, paymentMethod, installments)))
                .andExpect(status().isCreated());
    }
}
