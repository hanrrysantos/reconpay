package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import br.com.hanrry.reconpay.util.TestCnpj;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DemoQuotaGuardIntegrationTest extends AbstractIntegrationTest {

    private static final String MERCHANT_QUOTA = "A demonstração permite no máximo 2 empresas.";
    private static final String IMPORT_QUOTA = "A demonstração permite no máximo 10 importações.";

    private enum Endpoint {
        TRANSACTIONS("transactions", "externalReference,amount,paymentMethod,installments,status,transactionDate",
                "%s,100.00,PIX,1,APPROVED,2026-07-10"),
        SETTLEMENTS("external-settlements",
                "externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate",
                "%s,100.00,99.01,PIX,1,APPROVED,2026-07-10"),
        BANK_STATEMENTS("bank-statements", "lineReference,externalReference,amount,movementDate",
                "%s,,99.01,2026-07-10");

        private final String path;
        private final String header;
        private final String row;

        Endpoint(String path, String header, String row) {
            this.path = path;
            this.header = header;
            this.row = row;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoPurger purger;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<UUID> demos = new ArrayList<>();

    @AfterEach
    void purgeDemos() {
        demos.forEach(purger::purge);
        demos.clear();
    }

    @Test
    void demoCreatesASecondMerchantButNotAThird() throws Exception {
        String token = createDemo();

        createMerchant(token).andExpect(status().isCreated());
        createMerchant(token)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value(MERCHANT_QUOTA));

        mockMvc.perform(get("/api/merchants").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // The three scenario imports and a CSV refused as invalid leave all ten imports to the visitor.
    @Test
    void demoMakesTenImportsAndTheEleventhIsRefusedOnEveryEndpoint() throws Exception {
        String token = createDemo();
        String merchantId = onlyMerchantOf(token);

        importCsv(token, merchantId, Endpoint.TRANSACTIONS, Endpoint.TRANSACTIONS.header)
                .andExpect(status().isBadRequest());

        Endpoint[] accepted = {
                Endpoint.TRANSACTIONS, Endpoint.SETTLEMENTS, Endpoint.BANK_STATEMENTS,
                Endpoint.TRANSACTIONS, Endpoint.SETTLEMENTS, Endpoint.BANK_STATEMENTS,
                Endpoint.TRANSACTIONS, Endpoint.SETTLEMENTS, Endpoint.BANK_STATEMENTS,
                Endpoint.TRANSACTIONS};
        for (Endpoint endpoint : accepted) {
            importRow(token, merchantId, endpoint).andExpect(status().isCreated());
        }

        for (Endpoint endpoint : Endpoint.values()) {
            int batches = batchesOf(merchantId, endpoint);
            importRow(token, merchantId, endpoint)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.message").value(IMPORT_QUOTA));
            assertThat(batchesOf(merchantId, endpoint)).isEqualTo(batches);
        }
        assertThat(jdbc.queryForObject("SELECT demo_import_count FROM users WHERE id = ?", Integer.class,
                demos.getFirst())).isEqualTo(10);
    }

    @Test
    void regularAccountIsNotBoundByTheDemoQuotas() throws Exception {
        String token = IntegrationTestUtils.obtainOperatorToken(mockMvc);

        List<String> merchantIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String created = createMerchant(token).andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            merchantIds.add(JsonPath.read(created, "$.id"));
        }
        String merchantId = merchantIds.getFirst();
        mockMvc.perform(post("/api/merchants/{id}/fee-rules", merchantId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"PIX","installments":1,"feePercentage":0.99,"fixedFee":0.00}
                                """))
                .andExpect(status().isCreated());

        for (int i = 0; i < 11; i++) {
            importRow(token, merchantId, Endpoint.values()[i % 3]).andExpect(status().isCreated());
        }
    }

    private String createDemo() throws Exception {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String ip = "10.%d.%d.%d".formatted(random.nextInt(256), random.nextInt(256), random.nextInt(1, 255));
        String created = mockMvc.perform(post("/api/auth/demo").with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                }))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(created, "$.token");
        String me = mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        demos.add(UUID.fromString(JsonPath.read(me, "$.id")));
        return token;
    }

    private String onlyMerchantOf(String token) throws Exception {
        String merchants = mockMvc.perform(get("/api/merchants").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(merchants, "$.content[0].id");
    }

    private ResultActions createMerchant(String token) throws Exception {
        return mockMvc.perform(post("/api/merchants")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "Loja Cota", "document": "%s"}
                        """.formatted(TestCnpj.random())));
    }

    private ResultActions importRow(String token, String merchantId, Endpoint endpoint) throws Exception {
        String reference = "QUOTA-" + UUID.randomUUID();
        return importCsv(token, merchantId, endpoint, endpoint.header + "\n" + endpoint.row.formatted(reference));
    }

    private ResultActions importCsv(String token, String merchantId, Endpoint endpoint, String csv)
            throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "quota.csv", "text/csv", (csv + "\n").getBytes(StandardCharsets.UTF_8));
        return mockMvc.perform(multipart("/api/merchants/{id}/{path}/import", merchantId, endpoint.path)
                .file(file)
                .header("Authorization", "Bearer " + token));
    }

    private int batchesOf(String merchantId, Endpoint endpoint) {
        String table = switch (endpoint) {
            case TRANSACTIONS -> "transaction_imports";
            case SETTLEMENTS -> "settlement_imports";
            case BANK_STATEMENTS -> "bank_statement_imports";
        };
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE merchant_id = ?", Integer.class,
                UUID.fromString(merchantId));
    }
}
