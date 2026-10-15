package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * July holds two runs (the first superseded by the second), a window that only shares
 * July's start or end holds one each, and August holds one: an exact-window filter
 * returns July's two runs and nothing that merely overlaps it.
 */
@AutoConfigureMockMvc
class RunListIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate JULY_1 = LocalDate.of(2026, 7, 1);
    private static final LocalDate JULY_31 = LocalDate.of(2026, 7, 31);

    @Autowired MockMvc mockMvc;
    @Autowired IMerchantRepository merchants;
    @Autowired IReconciliationRunRepository runs;

    private String token;
    private UUID merchantId;
    private final List<UUID> julyRuns = new ArrayList<>();

    @BeforeEach
    void seed() throws Exception {
        token = IntegrationTestUtils.obtainAdminToken(mockMvc);
        MerchantEntity merchant = new MerchantEntity();
        merchant.setName("Run list merchant");
        merchant.setDocument(UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        merchant = merchants.saveAndFlush(merchant);
        merchantId = merchant.getId();

        ReconciliationRunEntity superseded = run(merchant, JULY_1, JULY_31);
        julyRuns.add(superseded.getId());
        run(merchant, JULY_1, LocalDate.of(2026, 7, 15));
        run(merchant, LocalDate.of(2026, 7, 16), JULY_31);
        run(merchant, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        superseded.setSupersededAt(Instant.now());
        runs.saveAndFlush(superseded);
        julyRuns.add(run(merchant, JULY_1, JULY_31).getId());
    }

    @Test
    void exactWindowFilterReturnsOnlyThatWindowsRunsNewestFirst() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + token)
                        .param("fromDate", "2026-07-01")
                        .param("toDate", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id")
                        .value(contains(julyRuns.get(1).toString(), julyRuns.get(0).toString())))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void exactWindowFilterWithSizeOneReturnsTheLatestRun() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + token)
                        .param("fromDate", "2026-07-01")
                        .param("toDate", "2026-07-31")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(contains(julyRuns.get(1).toString())));
    }

    @Test
    void windowWithoutRunsReturnsAnEmptyPage() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + token)
                        .param("fromDate", "2026-09-01")
                        .param("toDate", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void withoutWindowListsEveryRunOfTheMerchant() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void onlyOneWindowBoundIsRejected() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + token)
                        .param("fromDate", "2026-07-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("fromDate e toDate devem ser informados juntos"));

        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + token)
                        .param("toDate", "2026-07-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void malformedWindowDateIsRejected() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + token)
                        .param("fromDate", "07/01/2026")
                        .param("toDate", "2026-07-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownSortPropertyIsRejectedAsBadRequest() throws Exception {
        mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + token)
                        .param("sort", "nope,desc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Parâmetro de ordenação inválido: nope"));
    }

    private ReconciliationRunEntity run(MerchantEntity merchant, LocalDate fromDate, LocalDate toDate) {
        ReconciliationRunEntity run = new ReconciliationRunEntity();
        run.setMerchant(merchant);
        run.setFromDate(fromDate);
        run.setToDate(toDate);
        run.setStatus(ReconciliationRunStatus.COMPLETED);
        run.setTotalItems(0);
        run.setMatchedCount(0);
        run.setDivergentCount(0);
        return runs.saveAndFlush(run);
    }
}
