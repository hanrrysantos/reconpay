package br.com.hanrry.reconpay.reconciliation.repository;

import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface IReconciliationItemRepository extends
        JpaRepository<ReconciliationItemEntity, UUID>,
        JpaSpecificationExecutor<ReconciliationItemEntity> {

    /*
     * The export reads ids first and then hydrates them in chunks. Join fetching
     * a collection alongside pagination would force Hibernate to paginate in
     * memory, which is exactly what the export needs to avoid.
     */
    @Query("""
            SELECT item.id FROM ReconciliationItemEntity item
            WHERE item.reconciliationRun.id = :runId
            ORDER BY item.externalReference ASC
            """)
    Slice<UUID> findIdsByRunId(@Param("runId") UUID runId, Pageable pageable);

    @Query("""
            SELECT DISTINCT item FROM ReconciliationItemEntity item
            LEFT JOIN FETCH item.discrepancies
            WHERE item.id IN :ids
            ORDER BY item.externalReference ASC
            """)
    List<ReconciliationItemEntity> findAllWithDiscrepanciesByIdIn(@Param("ids") List<UUID> ids);

    /*
     * Orphan settlements carry no transaction amounts, so they drop out of
     * the sale sums on their own; their bank credit is excluded too, so the
     * received total matches the daily series keyed by sale date.
     */
    @Query("""
            SELECT new br.com.hanrry.reconpay.reconciliation.repository.RunSummaryTotals(
                SUM(item.transactionAmount),
                SUM(item.expectedNetAmount),
                SUM(item.transactionAmount - item.expectedNetAmount),
                SUM(CASE WHEN item.transactionDate IS NOT NULL THEN item.bankAmount END))
            FROM ReconciliationItemEntity item
            WHERE item.reconciliationRun.id = :runId
            """)
    RunSummaryTotals sumRunTotals(@Param("runId") UUID runId);

    /*
     * Settled net short of the expected net, only where the engine flagged
     * a fee divergence; a settlement above the expected net counts as zero.
     */
    @Query("""
            SELECT SUM(CASE WHEN item.expectedNetAmount > item.settlementNetAmount
                            THEN item.expectedNetAmount - item.settlementNetAmount
                            ELSE 0 END)
            FROM ReconciliationItemEntity item
            WHERE item.reconciliationRun.id = :runId
              AND EXISTS (
                  SELECT discrepancy.id FROM ReconciliationDiscrepancyEntity discrepancy
                  WHERE discrepancy.reconciliationItem = item
                    AND discrepancy.type = br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType.FEE_DIVERGENCE)
            """)
    BigDecimal sumFeeAboveContract(@Param("runId") UUID runId);

    /* Days without items are absent; orphan settlements have no sale date. */
    @Query("""
            SELECT new br.com.hanrry.reconpay.reconciliation.repository.DailySummaryRow(
                item.transactionDate,
                SUM(item.expectedNetAmount),
                SUM(item.settlementNetAmount),
                SUM(item.bankAmount),
                COUNT(item),
                SUM(CASE WHEN item.result = br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult.MATCHED
                         THEN 1 ELSE 0 END))
            FROM ReconciliationItemEntity item
            WHERE item.reconciliationRun.id = :runId
              AND item.transactionDate IS NOT NULL
            GROUP BY item.transactionDate
            ORDER BY item.transactionDate
            """)
    List<DailySummaryRow> sumByTransactionDate(@Param("runId") UUID runId);
}
