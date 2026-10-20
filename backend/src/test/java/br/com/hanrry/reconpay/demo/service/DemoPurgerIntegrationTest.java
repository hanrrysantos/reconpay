package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.security.JwtService;
import br.com.hanrry.reconpay.util.TestCnpj;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DemoPurgerIntegrationTest extends AbstractIntegrationTest {

    private static final String WINDOW = """
            {"fromDate":"2026-07-01","toDate":"2026-07-15"}
            """;

    // Rows of one merchant in every table that holds merchant data, written from the schema.
    private static final Map<String, String> MERCHANT_ROWS = new LinkedHashMap<>();

    static {
        String runs = "SELECT id FROM reconciliation_runs WHERE merchant_id = ?";
        String items = "SELECT id FROM reconciliation_items WHERE reconciliation_run_id IN (" + runs + ")";
        String discrepancies = "SELECT id FROM reconciliation_discrepancies WHERE reconciliation_item_id IN ("
                + items + ")";
        MERCHANT_ROWS.put("merchants", "SELECT count(*) FROM merchants WHERE id = ?");
        MERCHANT_ROWS.put("user_merchants", "SELECT count(*) FROM user_merchants WHERE merchant_id = ?");
        for (String table : List.of("fee_rules", "transaction_imports", "internal_transactions",
                "settlement_imports", "external_settlements", "bank_statement_imports", "bank_statement_lines",
                "reconciliation_runs", "period_locks")) {
            MERCHANT_ROWS.put(table, "SELECT count(*) FROM " + table + " WHERE merchant_id = ?");
        }
        MERCHANT_ROWS.put("reconciliation_items", "SELECT count(*) FROM (" + items + ") rows");
        MERCHANT_ROWS.put("reconciliation_discrepancies", "SELECT count(*) FROM (" + discrepancies + ") rows");
        MERCHANT_ROWS.put("discrepancy_adjustments",
                "SELECT count(*) FROM discrepancy_adjustments WHERE discrepancy_id IN (" + discrepancies + ")");
        MERCHANT_ROWS.put("discrepancy_transitions",
                "SELECT count(*) FROM discrepancy_transitions WHERE discrepancy_id IN (" + discrepancies + ")");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DemoPurger purger;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void purgeDeletesEveryRowOfADemoOnlyMerchantAndTheUser() throws Exception {
        UserEntity demo = saveUser(Instant.now().plus(Duration.ofHours(1)));
        UUID merchantId = populatedMerchant(demo, true);
        UserEntity otherDemo = saveUser(Instant.now().plus(Duration.ofHours(1)));
        UUID otherMerchantId = populatedMerchant(otherDemo, true);
        Map<String, Long> otherBefore = rows(otherMerchantId);

        assertThat(rows(merchantId)).allSatisfy((table, count) -> assertThat(count).as(table).isPositive());

        int purgedMerchants = purger.purge(demo.getId());

        assertThat(purgedMerchants).isEqualTo(1);
        assertThat(rows(merchantId)).allSatisfy((table, count) -> assertThat(count).as(table).isZero());
        assertThat(userRepository.existsById(demo.getId())).isFalse();
        assertThat(rows(otherMerchantId)).isEqualTo(otherBefore);
        assertThat(userRepository.existsById(otherDemo.getId())).isTrue();
    }

    @Test
    void purgeKeepsAMerchantARegularAccountCanReachAndDropsOnlyTheDemoAccess() throws Exception {
        UserEntity demo = saveUser(Instant.now().plus(Duration.ofHours(1)));
        UUID merchantId = populatedMerchant(demo, false);
        UserEntity regular = saveUser(null);
        jdbc.update("INSERT INTO user_merchants (user_id, merchant_id, granted_at) VALUES (?, ?, now())",
                regular.getId(), merchantId);
        Map<String, Long> before = rows(merchantId);

        int purgedMerchants = purger.purge(demo.getId());

        assertThat(purgedMerchants).isZero();
        Map<String, Long> after = rows(merchantId);
        assertThat(after.get("user_merchants")).isEqualTo(1L);
        assertThat(accessHolders(merchantId)).containsExactly(regular.getId());
        before.remove("user_merchants");
        after.remove("user_merchants");
        assertThat(after).isEqualTo(before);
        assertThat(after.get("internal_transactions")).isPositive();
        assertThat(userRepository.existsById(demo.getId())).isFalse();
        assertThat(userRepository.existsById(regular.getId())).isTrue();
    }

    @Test
    void findExpiredReturnsOnlyDemosExpiredAtOrBeforeNow() {
        Instant now = Instant.parse("2031-03-01T12:00:00Z");
        UserEntity expired = saveUser(now.minusSeconds(1));
        UserEntity expiringNow = saveUser(now);
        UserEntity active = saveUser(now.plusSeconds(1));
        UserEntity regular = saveUser(null);

        List<UUID> found = purger.findExpired(now);

        assertThat(found).contains(expired.getId(), expiringNow.getId());
        assertThat(found).doesNotContain(active.getId(), regular.getId());
    }

    @Test
    void everyTableThatReachesMerchantsWithoutCascadeIsPurged() {
        List<Map<String, Object>> foreignKeys = jdbc.queryForList("""
                SELECT kcu.table_name AS child, ccu.table_name AS parent, rc.delete_rule AS delete_rule
                FROM information_schema.referential_constraints rc
                JOIN information_schema.key_column_usage kcu
                  ON kcu.constraint_name = rc.constraint_name AND kcu.constraint_schema = rc.constraint_schema
                JOIN information_schema.constraint_column_usage ccu
                  ON ccu.constraint_name = rc.constraint_name AND ccu.constraint_schema = rc.constraint_schema
                WHERE rc.constraint_schema = current_schema()
                """);
        Set<String> reaching = new HashSet<>(Set.of("merchants"));
        boolean grew = true;
        while (grew) {
            grew = false;
            for (Map<String, Object> fk : foreignKeys) {
                boolean cascades = "CASCADE".equals(fk.get("delete_rule"));
                if (!cascades && reaching.contains((String) fk.get("parent"))) {
                    grew |= reaching.add((String) fk.get("child"));
                }
            }
        }

        assertThat(reaching).hasSizeGreaterThan(10);
        assertThat(DemoPurger.purgedTables()).containsAll(reaching);
    }

    private UUID populatedMerchant(UserEntity demo, boolean resolveAndLock) throws Exception {
        String token = jwtService.generateToken(demo.getEmail());
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String matched = "TX-OK-" + suffix;
        String missing = "TX-MISS-" + suffix;

        String merchant = mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Loja Demonstração","document":"%s"}
                                """.formatted(TestCnpj.random())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID merchantId = UUID.fromString(JsonPath.read(merchant, "$.id"));

        mockMvc.perform(post("/api/merchants/{merchantId}/fee-rules", merchantId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"PIX","installments":1,"feePercentage":1.0000,"fixedFee":0.00}
                                """))
                .andExpect(status().isCreated());

        importCsv(token, "/api/merchants/{merchantId}/transactions/import", merchantId, """
                externalReference,amount,paymentMethod,installments,status,transactionDate
                %s,100.00,PIX,1,APPROVED,2026-07-10
                %s,100.00,PIX,1,APPROVED,2026-07-10
                """.formatted(matched, missing));
        importCsv(token, "/api/merchants/{merchantId}/external-settlements/import", merchantId, """
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                %s,100.00,99.00,PIX,1,APPROVED,2026-07-10
                """.formatted(matched));
        importCsv(token, "/api/merchants/{merchantId}/bank-statements/import", merchantId, """
                lineReference,externalReference,amount,movementDate
                BANK-%s,%s,99.00,2026-07-10
                """.formatted(suffix, matched));

        String run = mockMvc.perform(post("/api/merchants/{merchantId}/reconciliations", merchantId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(WINDOW))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String runId = JsonPath.read(run, "$.id");
        org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                mockMvc.perform(get("/api/merchants/{merchantId}/reconciliations/{runId}", merchantId, runId)
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("COMPLETED")));

        if (resolveAndLock) {
            List<UUID> discrepancyIds = jdbc.queryForList("""
                    SELECT d.id FROM reconciliation_discrepancies d
                    JOIN reconciliation_items i ON i.id = d.reconciliation_item_id
                    WHERE i.reconciliation_run_id = ?
                    """, UUID.class, UUID.fromString(runId));
            for (UUID discrepancyId : discrepancyIds) {
                mockMvc.perform(patch("/api/merchants/{merchantId}/reconciliations/{runId}/discrepancies/{id}",
                                merchantId, runId, discrepancyId)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"status":"ADJUSTED","correctionAmount":-1.00,"note":"demo"}
                                        """))
                        .andExpect(status().isOk());
            }
            mockMvc.perform(post("/api/merchants/{merchantId}/periods/lock", merchantId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(WINDOW))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.locked").value(true));
        }
        return merchantId;
    }

    private void importCsv(String token, String path, UUID merchantId, String csv) throws Exception {
        mockMvc.perform(multipart(path, merchantId)
                        .file(new MockMultipartFile("file", "demo.csv", "text/csv",
                                csv.getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    private Map<String, Long> rows(UUID merchantId) {
        Map<String, Long> counts = new LinkedHashMap<>();
        MERCHANT_ROWS.forEach((table, sql) -> counts.put(table, jdbc.queryForObject(sql, Long.class, merchantId)));
        return counts;
    }

    private List<UUID> accessHolders(UUID merchantId) {
        return jdbc.queryForList("SELECT user_id FROM user_merchants WHERE merchant_id = ?", UUID.class, merchantId);
    }

    private UserEntity saveUser(Instant demoExpiresAt) {
        UserEntity user = new UserEntity();
        user.setName("Visitante");
        user.setEmail("demo-" + UUID.randomUUID() + "@demo.reconpay.invalid");
        user.setPassword("not-used");
        user.setRole(UserRole.OPERATOR);
        user.setActive(true);
        user.setDemoExpiresAt(demoExpiresAt);
        return userRepository.save(user);
    }
}
