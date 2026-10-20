package br.com.hanrry.reconpay.demo.controller;

import br.com.hanrry.reconpay.bankstatement.service.BankStatementService;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.demo.service.DemoPurger;
import br.com.hanrry.reconpay.demo.service.DemoRunExecutor;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** DEMO-01 AC10: a failed creation answers 500 and leaves nothing behind. */
@AutoConfigureMockMvc
class DemoCreationFailureIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoPurger purger;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private BankStatementService bankStatementService;

    @MockitoSpyBean
    private DemoRunExecutor runExecutor;

    @Test
    void failureWhileProvisioningAnswers500AndLeavesNoUserOrMerchant() throws Exception {
        doThrow(new IllegalStateException("falha simulada"))
                .when(bankStatementService).importCsv(any(), any());
        assertFailedCreationLeavesNothingAndKeepsTheAddressQuota(bankStatementService);
    }

    @Test
    void failureWhileReconcilingAnswers500AndLeavesNoUserOrMerchant() throws Exception {
        // Fails after the real run settles, so the dispatcher never races the compensating purge.
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("falha simulada");
        }).when(runExecutor).complete(any());
        assertFailedCreationLeavesNothingAndKeepsTheAddressQuota(runExecutor);
    }

    private void assertFailedCreationLeavesNothingAndKeepsTheAddressQuota(Object failingSpy) throws Exception {
        String ip = nextIp();
        Map<String, Long> before = counts();

        createDemo(ip)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"));

        assertThat(counts()).isEqualTo(before);

        // The refused attempt does not count: the same address still gets both of its demos.
        Mockito.reset(failingSpy);
        for (int i = 0; i < 2; i++) {
            String token = JsonPath.read(createDemo(ip).andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString(), "$.token");
            purgeDemoOf(token);
        }
    }

    private Map<String, Long> counts() {
        return Map.of(
                "users", jdbc.queryForObject("SELECT count(*) FROM users", Long.class),
                "merchants", jdbc.queryForObject("SELECT count(*) FROM merchants", Long.class),
                "reconciliation_runs", jdbc.queryForObject("SELECT count(*) FROM reconciliation_runs", Long.class));
    }

    private ResultActions createDemo(String ip) throws Exception {
        return mockMvc.perform(post("/api/auth/demo").with(request -> {
            request.setRemoteAddr(ip);
            return request;
        }));
    }

    private static String nextIp() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return "10.%d.%d.%d".formatted(random.nextInt(256), random.nextInt(256), random.nextInt(1, 255));
    }

    // Keeps the shared database below the cap for the classes that run after this one.
    private void purgeDemoOf(String token) throws Exception {
        String me = mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        purger.purge(UUID.fromString(JsonPath.read(me, "$.id")));
    }
}
