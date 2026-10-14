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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.UUID;

import static br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus.ACCEPTED;
import static br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus.OPEN;
import static br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType.BANK_AMOUNT_MISMATCH;
import static br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType.FEE_DIVERGENCE;
import static br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType.MISSING_BANK_CREDIT;
import static br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType.MISSING_SETTLEMENT;
import static br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult.DIVERGENT;
import static br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult.MATCHED;
import static br.com.hanrry.reconpay.reconciliation.repository.ReconciliationItemSpecifications.withFilters;
import static org.assertj.core.api.Assertions.assertThat;

/*
 * Fixture, one run:
 *   A  DIVERGENT  FEE_DIVERGENCE OPEN + FEE_DIVERGENCE OPEN (same type twice)
 *   B  DIVERGENT  MISSING_SETTLEMENT ACCEPTED
 *   C  DIVERGENT  MISSING_BANK_CREDIT OPEN + FEE_DIVERGENCE ACCEPTED
 *   D  MATCHED    none
 *   E  MATCHED    BANK_AMOUNT_MISMATCH OPEN
 * plus an OPEN item in another run that must never leak in.
 */
class RunItemsFilterIntegrationTest extends AbstractIntegrationTest {

    private static final PageRequest BY_REFERENCE = PageRequest.of(0, 20, Sort.by("externalReference"));

    @Autowired IMerchantRepository merchants;
    @Autowired IReconciliationRunRepository runs;
    @Autowired IReconciliationItemRepository items;

    private UUID runId;

    @BeforeEach
    void seed() {
        MerchantEntity merchant = merchant();
        ReconciliationRunEntity run = completedRun(merchant, LocalDate.of(2026, 6, 1));
        runId = run.getId();

        item(run, "A", DIVERGENT, discrepancy(FEE_DIVERGENCE, OPEN), discrepancy(FEE_DIVERGENCE, OPEN));
        item(run, "B", DIVERGENT, discrepancy(MISSING_SETTLEMENT, ACCEPTED));
        item(run, "C", DIVERGENT, discrepancy(MISSING_BANK_CREDIT, OPEN), discrepancy(FEE_DIVERGENCE, ACCEPTED));
        item(run, "D", MATCHED);
        item(run, "E", MATCHED, discrepancy(BANK_AMOUNT_MISMATCH, OPEN));

        ReconciliationRunEntity otherRun = completedRun(merchant, LocalDate.of(2026, 5, 1));
        item(otherRun, "A", DIVERGENT, discrepancy(FEE_DIVERGENCE, OPEN));
    }

    @Test
    void openStatusReturnsOnlyItemsWithAnOpenDiscrepancyEachOnce() {
        Page<ReconciliationItemEntity> page = find(null, null, OPEN);

        assertThat(references(page)).containsExactly("A", "C", "E");
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void statusFilterMatchesNonOpenStatusesToo() {
        Page<ReconciliationItemEntity> page = find(null, null, ACCEPTED);

        assertThat(references(page)).containsExactly("B", "C");
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void itemWithTheSameTypeTwiceAppearsOnce() {
        Page<ReconciliationItemEntity> page = find(null, FEE_DIVERGENCE, null);

        assertThat(references(page)).containsExactly("A", "C");
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void typeAndStatusMustHoldOnTheSameDiscrepancy() {
        Page<ReconciliationItemEntity> page = find(null, FEE_DIVERGENCE, OPEN);

        assertThat(references(page)).containsExactly("A");
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    void resultCombinesWithStatus() {
        assertThat(references(find(DIVERGENT, null, OPEN))).containsExactly("A", "C");
        assertThat(references(find(MATCHED, null, OPEN))).containsExactly("E");
    }

    @Test
    void resultTypeAndStatusCombine() {
        assertThat(references(find(DIVERGENT, MISSING_BANK_CREDIT, OPEN))).containsExactly("C");
        assertThat(references(find(MATCHED, MISSING_BANK_CREDIT, OPEN))).isEmpty();
        assertThat(references(find(DIVERGENT, MISSING_SETTLEMENT, OPEN))).isEmpty();
    }

    @Test
    void noDiscrepancyFilterKeepsEveryItemOfTheRun() {
        assertThat(references(find(null, null, null))).containsExactly("A", "B", "C", "D", "E");
    }

    private Page<ReconciliationItemEntity> find(
            ReconciliationResult result, DiscrepancyType type, DiscrepancyStatus status) {
        return items.findAll(withFilters(runId, result, type, status), BY_REFERENCE);
    }

    private static java.util.List<String> references(Page<ReconciliationItemEntity> page) {
        return page.getContent().stream().map(ReconciliationItemEntity::getExternalReference).toList();
    }

    private MerchantEntity merchant() {
        MerchantEntity merchant = new MerchantEntity();
        merchant.setName("Items filter merchant");
        merchant.setDocument(UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        return merchants.saveAndFlush(merchant);
    }

    private ReconciliationRunEntity completedRun(MerchantEntity merchant, LocalDate fromDate) {
        ReconciliationRunEntity run = new ReconciliationRunEntity();
        run.setMerchant(merchant);
        run.setFromDate(fromDate);
        run.setToDate(fromDate.plusDays(29));
        run.setStatus(ReconciliationRunStatus.COMPLETED);
        run.setTotalItems(0);
        run.setMatchedCount(0);
        run.setDivergentCount(0);
        return runs.saveAndFlush(run);
    }

    private void item(
            ReconciliationRunEntity run,
            String reference,
            ReconciliationResult result,
            ReconciliationDiscrepancyEntity... discrepancies) {
        ReconciliationItemEntity item = new ReconciliationItemEntity();
        item.setReconciliationRun(run);
        item.setExternalReference(reference);
        item.setResult(result);
        for (ReconciliationDiscrepancyEntity discrepancy : discrepancies) {
            item.addDiscrepancy(discrepancy);
        }
        items.saveAndFlush(item);
    }

    private static ReconciliationDiscrepancyEntity discrepancy(DiscrepancyType type, DiscrepancyStatus status) {
        ReconciliationDiscrepancyEntity discrepancy = new ReconciliationDiscrepancyEntity();
        discrepancy.setType(type);
        discrepancy.setStatus(status);
        return discrepancy;
    }
}
