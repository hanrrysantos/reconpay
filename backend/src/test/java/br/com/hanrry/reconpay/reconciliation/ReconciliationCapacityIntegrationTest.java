package br.com.hanrry.reconpay.reconciliation;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import br.com.hanrry.reconpay.util.TestCnpj;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CAP-01 against the real counts: transactions of the window plus settlements and bank lines of the
 * window extended by settlement-lag-days (5), with the limit lowered to 10 records.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "reconpay.reconciliation.max-window-records=10")
class ReconciliationCapacityIntegrationTest extends AbstractIntegrationTest {

    private static final String WINDOW = """
            {"fromDate": "2026-07-01", "toDate": "2026-07-31"}""";

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = IntegrationTestUtils.obtainAdminToken(mockMvc);
    }

    @Test
    void aWindowAtTheLimitIsAcceptedAndOneRecordMoreIsRefusedWithoutCreatingARun() throws Exception {
        String merchant = merchantWithTenRecordsInTheWindow();

        String runId = JsonPath.read(requestRun(merchant).andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString(), "$.id");
        awaitStatus(merchant, runId, "COMPLETED");

        importBankLines(merchant, "BANK-EXTRA,,15.00,2026-08-04");

        requestRun(merchant)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(
                        "A janela tem 11 registros para conciliar; o limite é 10. Divida o período em janelas menores."));
        runsOfTheWindow(merchant).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void anotherMerchantOverTheLimitDoesNotCountTowardThisOne() throws Exception {
        String crowded = merchantWithTenRecordsInTheWindow();
        importBankLines(crowded, "BANK-EXTRA,,15.00,2026-07-20");
        String quiet = merchantWithTenRecordsInTheWindow();

        requestRun(crowded).andExpect(status().isBadRequest());
        runsOfTheWindow(crowded).andExpect(jsonPath("$.totalElements").value(0));
        requestRun(quiet).andExpect(status().isAccepted());
    }

    /*
     * 4 sales in July, 3 payouts and 3 bank lines (the last of each on Aug 5, the end of the lag) = 10.
     * One sale on Aug 2 and the payout and bank line on Aug 6 fall outside and must not count.
     */
    private String merchantWithTenRecordsInTheWindow() throws Exception {
        String merchant = JsonPath.read(mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Capacidade\",\"document\":\"%s\"}".formatted(TestCnpj.random())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(post("/api/merchants/{merchantId}/fee-rules", merchant)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"PIX\",\"installments\":1,\"feePercentage\":1.0000,\"fixedFee\":0.00}"))
                .andExpect(status().isCreated());

        String tag = UUID.randomUUID().toString().substring(0, 8);
        upload(merchant, "/transactions/import", """
                externalReference,amount,paymentMethod,installments,status,transactionDate
                %1$s-1,100.00,PIX,1,APPROVED,2026-07-10
                %1$s-2,100.00,PIX,1,APPROVED,2026-07-11
                %1$s-3,100.00,PIX,1,APPROVED,2026-07-12
                %1$s-4,100.00,PIX,1,APPROVED,2026-07-31
                %1$s-5,100.00,PIX,1,APPROVED,2026-08-02
                """.formatted(tag));
        upload(merchant, "/external-settlements/import", """
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                %1$s-1,100.00,99.00,PIX,1,APPROVED,2026-07-10
                %1$s-2,100.00,99.00,PIX,1,APPROVED,2026-07-11
                %1$s-4,100.00,99.00,PIX,1,APPROVED,2026-08-05
                %1$s-5,100.00,99.00,PIX,1,APPROVED,2026-08-06
                """.formatted(tag));
        importBankLines(merchant,
                "BANK-%s-1,%s-1,99.00,2026-07-10".formatted(tag, tag),
                "BANK-%s-2,%s-2,99.00,2026-07-11".formatted(tag, tag),
                "BANK-%s-4,%s-4,99.00,2026-08-05".formatted(tag, tag),
                "BANK-%s-5,%s-5,99.00,2026-08-06".formatted(tag, tag));
        return merchant;
    }

    private void importBankLines(String merchant, String... lines) throws Exception {
        String prefix = UUID.randomUUID().toString().substring(0, 4);
        StringBuilder csv = new StringBuilder("lineReference,externalReference,amount,movementDate\n");
        for (String line : lines) {
            csv.append(line.startsWith("BANK-EXTRA") ? line.replace("BANK-EXTRA", "BANK-EXTRA-" + prefix) : line).append('\n');
        }
        upload(merchant, "/bank-statements/import", csv.toString());
    }

    private void upload(String merchant, String path, String csv) throws Exception {
        mockMvc.perform(multipart("/api/merchants/{merchantId}" + path, merchant)
                        .file(new MockMultipartFile("file", "file.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());
    }

    private ResultActions requestRun(String merchant) throws Exception {
        return mockMvc.perform(post("/api/merchants/{merchantId}/reconciliations", merchant)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(WINDOW));
    }

    private ResultActions runsOfTheWindow(String merchant) throws Exception {
        return mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations", merchant)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("fromDate", "2026-07-01")
                        .param("toDate", "2026-07-31"))
                .andExpect(status().isOk());
    }

    private void awaitStatus(String merchant, String runId, String expected) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchant, runId)
                                .header("Authorization", "Bearer " + adminToken))
                        .andExpect(jsonPath("$.status").value(expected)));
    }
}
