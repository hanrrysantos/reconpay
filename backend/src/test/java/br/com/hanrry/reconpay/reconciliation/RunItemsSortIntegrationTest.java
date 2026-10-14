package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationItemRepository;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * |settlementNet - expectedNet| per item, missing side as zero:
 *   R4  100.00 vs 110.00 -> 10.00  OPEN   (saved before R1 to prove the tiebreak)
 *   R1  100.00 vs  90.00 -> 10.00  OPEN
 *   R2   50.00 vs  null  -> 50.00  OPEN   (no settlement)
 *   R3   null  vs  30.00 -> 30.00  OPEN   (orphan settlement)
 *   R5   20.00 vs  19.99 ->  0.01  OPEN
 *   R6    0.00 vs 1000.00 -> 1000.00 ACCEPTED only
 */
@AutoConfigureMockMvc
class RunItemsSortIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired IMerchantRepository merchants;
    @Autowired IReconciliationRunRepository runs;
    @Autowired IReconciliationItemRepository items;

    private String token;
    private UUID merchantId;
    private UUID runId;

    @BeforeEach
    void seed() throws Exception {
        token = IntegrationTestUtils.obtainAdminToken(mockMvc);
        MerchantEntity merchant = new MerchantEntity();
        merchant.setName("Items sort merchant");
        merchant.setDocument(UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        merchant = merchants.saveAndFlush(merchant);
        merchantId = merchant.getId();

        ReconciliationRunEntity run = new ReconciliationRunEntity();
        run.setMerchant(merchant);
        run.setFromDate(LocalDate.of(2026, 6, 1));
        run.setToDate(LocalDate.of(2026, 6, 30));
        run.setStatus(ReconciliationRunStatus.COMPLETED);
        run.setTotalItems(6);
        run.setMatchedCount(0);
        run.setDivergentCount(6);
        run = runs.saveAndFlush(run);
        runId = run.getId();

        item(run, "R4", "100.00", "110.00", DiscrepancyType.FEE_DIVERGENCE, DiscrepancyStatus.OPEN);
        item(run, "R1", "100.00", "90.00", DiscrepancyType.FEE_DIVERGENCE, DiscrepancyStatus.OPEN);
        item(run, "R2", "50.00", null, DiscrepancyType.MISSING_SETTLEMENT, DiscrepancyStatus.OPEN);
        item(run, "R3", null, "30.00", DiscrepancyType.ORPHAN_SETTLEMENT, DiscrepancyStatus.OPEN);
        item(run, "R5", "20.00", "19.99", DiscrepancyType.FEE_DIVERGENCE, DiscrepancyStatus.OPEN);
        item(run, "R6", "0.00", "1000.00", DiscrepancyType.INCORRECT_AMOUNT, DiscrepancyStatus.ACCEPTED);
    }

    @Test
    void openItemsSortedByLargestAbsoluteDifferenceWithReferenceTiebreak() throws Exception {
        items("OPEN", "absDifference,desc", 0, 4)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].externalReference").value(contains("R2", "R3", "R1", "R4")))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2));

        items("OPEN", "absDifference,desc", 1, 4)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].externalReference").value(contains("R5")));
    }

    @Test
    void ascendingKeepsMissingSidesAsZeroAndReferenceTiebreak() throws Exception {
        items("OPEN", "absDifference,asc", 0, 20)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].externalReference").value(contains("R5", "R1", "R4", "R3", "R2")));
    }

    @Test
    void sortCombinesWithTypeFilter() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}/items", merchantId, runId)
                        .header("Authorization", "Bearer " + token)
                        .param("discrepancyStatus", "OPEN")
                        .param("discrepancyType", "FEE_DIVERGENCE")
                        .param("result", "DIVERGENT")
                        .param("sort", "absDifference,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].externalReference").value(contains("R1", "R4", "R5")))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void withoutStatusTheLargestDifferenceOfAnyStatusComesFirst() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}/items", merchantId, runId)
                        .header("Authorization", "Bearer " + token)
                        .param("sort", "absDifference,desc")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].externalReference").value(contains("R6", "R2")))
                .andExpect(jsonPath("$.totalElements").value(6));
    }

    @Test
    void invalidDiscrepancyStatusReturnsValidationError() throws Exception {
        items("RESOLVED", "absDifference,desc", 0, 4)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    private ResultActions items(String status, String sort, int page, int size) throws Exception {
        return mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}/items", merchantId, runId)
                .header("Authorization", "Bearer " + token)
                .param("discrepancyStatus", status)
                .param("sort", sort)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)));
    }

    private void item(
            ReconciliationRunEntity run,
            String reference,
            String expectedNet,
            String settledNet,
            DiscrepancyType type,
            DiscrepancyStatus status) {
        ReconciliationItemEntity item = new ReconciliationItemEntity();
        item.setReconciliationRun(run);
        item.setExternalReference(reference);
        item.setResult(ReconciliationResult.DIVERGENT);
        item.setExpectedNetAmount(expectedNet == null ? null : new BigDecimal(expectedNet));
        item.setSettlementNetAmount(settledNet == null ? null : new BigDecimal(settledNet));
        ReconciliationDiscrepancyEntity discrepancy = new ReconciliationDiscrepancyEntity();
        discrepancy.setType(type);
        discrepancy.setStatus(status);
        item.addDiscrepancy(discrepancy);
        items.saveAndFlush(item);
    }
}
