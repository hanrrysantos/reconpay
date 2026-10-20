package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.observability.AuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 * Deletes a demo account and every merchant only demo accounts can reach. A merchant that a
 * regular account can also reach stays with all its data; only the demo's access goes, by
 * cascade, with the user.
 */
@Service
@RequiredArgsConstructor
public class DemoPurger {

    private static final String RUNS = "SELECT id FROM reconciliation_runs WHERE merchant_id IN (:merchantIds)";
    private static final String ITEMS = "SELECT id FROM reconciliation_items WHERE reconciliation_run_id IN (" + RUNS + ")";
    private static final String DISCREPANCIES =
            "SELECT id FROM reconciliation_discrepancies WHERE reconciliation_item_id IN (" + ITEMS + ")";
    private static final String BY_MERCHANT = "merchant_id IN (:merchantIds)";

    private record Step(String table, String condition) {
    }

    // Children before parents; user_merchants goes by ON DELETE CASCADE with the merchant.
    private static final List<Step> STEPS = List.of(
            new Step("discrepancy_transitions", "discrepancy_id IN (" + DISCREPANCIES + ")"),
            new Step("discrepancy_adjustments", "discrepancy_id IN (" + DISCREPANCIES + ")"),
            new Step("reconciliation_discrepancies", "reconciliation_item_id IN (" + ITEMS + ")"),
            new Step("period_locks", BY_MERCHANT),
            new Step("reconciliation_items", "reconciliation_run_id IN (" + RUNS + ")"),
            new Step("reconciliation_runs", BY_MERCHANT),
            new Step("bank_statement_lines", BY_MERCHANT),
            new Step("bank_statement_imports", BY_MERCHANT),
            new Step("external_settlements", BY_MERCHANT),
            new Step("settlement_imports", BY_MERCHANT),
            new Step("internal_transactions", BY_MERCHANT),
            new Step("transaction_imports", BY_MERCHANT),
            new Step("fee_rules", BY_MERCHANT),
            new Step("merchants", "id IN (:merchantIds)"));

    // Merchants of the user that no regular account (demo_expires_at null) can reach.
    private static final String DEMO_ONLY_MERCHANTS = """
            SELECT access.merchant_id
            FROM user_merchants access
            WHERE access.user_id = :userId
              AND NOT EXISTS (
                  SELECT 1
                  FROM user_merchants other
                  JOIN users holder ON holder.id = other.user_id
                  WHERE other.merchant_id = access.merchant_id
                    AND holder.demo_expires_at IS NULL)
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final AuditLogger auditLogger;

    /** Tables the purge deletes from, in delete order. */
    static List<String> purgedTables() {
        return STEPS.stream().map(Step::table).toList();
    }

    /** Deletes the demo user and its demo-only merchants; returns how many merchants went. */
    @Transactional
    public int purge(UUID userId) {
        List<UUID> merchantIds = jdbc.queryForList(DEMO_ONLY_MERCHANTS, Map.of("userId", userId), UUID.class);
        if (!merchantIds.isEmpty()) {
            Map<String, Object> params = Map.of("merchantIds", merchantIds);
            for (Step step : STEPS) {
                jdbc.update("DELETE FROM " + step.table() + " WHERE " + step.condition(), params);
            }
        }
        jdbc.update("DELETE FROM users WHERE id = :userId", Map.of("userId", userId));

        auditLogger.record("DEMO_PURGED", "user", userId, "merchants=" + merchantIds.size());
        return merchantIds.size();
    }

    @Transactional(readOnly = true)
    public List<UUID> findExpired(Instant now) {
        return jdbc.queryForList(
                "SELECT id FROM users WHERE demo_expires_at <= :now ORDER BY demo_expires_at",
                Map.of("now", now.atOffset(ZoneOffset.UTC)),
                UUID.class);
    }
}
