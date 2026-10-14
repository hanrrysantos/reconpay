package br.com.hanrry.reconpay.reconciliation.repository;

import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IReconciliationDiscrepancyRepository extends JpaRepository<ReconciliationDiscrepancyEntity, UUID> {

    @Query("""
            SELECT discrepancy FROM ReconciliationDiscrepancyEntity discrepancy
            JOIN FETCH discrepancy.reconciliationItem item
            WHERE item.reconciliationRun.id = :runId
            """)
    List<ReconciliationDiscrepancyEntity> findByRunIdWithItem(@Param("runId") UUID runId);

    @Query("""
            SELECT discrepancy FROM ReconciliationDiscrepancyEntity discrepancy
            JOIN discrepancy.reconciliationItem item
            JOIN item.reconciliationRun run
            WHERE discrepancy.id = :discrepancyId
              AND run.id = :runId
              AND run.merchant.id = :merchantId
            """)
    Optional<ReconciliationDiscrepancyEntity> findByIdAndRunAndMerchant(
            @Param("discrepancyId") UUID discrepancyId,
            @Param("runId") UUID runId,
            @Param("merchantId") UUID merchantId);

    @Query("""
            SELECT new br.com.hanrry.reconpay.reconciliation.repository.DiscrepancyTypeCount(
                discrepancy.type, COUNT(discrepancy))
            FROM ReconciliationDiscrepancyEntity discrepancy
            WHERE discrepancy.reconciliationItem.reconciliationRun.id = :runId
              AND discrepancy.status = br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus.OPEN
            GROUP BY discrepancy.type
            """)
    List<DiscrepancyTypeCount> countOpenByType(@Param("runId") UUID runId);

    /* Resolved = any outcome other than OPEN: ACCEPTED, ADJUSTED or WRITTEN_OFF. */
    @Query("""
            SELECT COUNT(discrepancy)
            FROM ReconciliationDiscrepancyEntity discrepancy
            WHERE discrepancy.reconciliationItem.reconciliationRun.id = :runId
              AND discrepancy.status <> br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus.OPEN
            """)
    long countResolved(@Param("runId") UUID runId);
}
