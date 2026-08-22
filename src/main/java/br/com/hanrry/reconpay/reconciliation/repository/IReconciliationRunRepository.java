package br.com.hanrry.reconpay.reconciliation.repository;

import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface IReconciliationRunRepository extends JpaRepository<ReconciliationRunEntity, UUID> {

    Page<ReconciliationRunEntity> findAllByMerchant_Id(UUID merchantId, Pageable pageable);

    Optional<ReconciliationRunEntity> findByIdAndMerchant_Id(UUID id, UUID merchantId);

    /*
     * Only completed runs hold the current-result slot, and the run being
     * finished must not supersede itself.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE ReconciliationRunEntity run
            SET run.supersededAt = :supersededAt
            WHERE run.merchant.id = :merchantId
              AND run.fromDate = :fromDate
              AND run.toDate = :toDate
              AND run.id <> :currentRunId
              AND run.status = br.com.hanrry.reconpay.reconciliation.enums.ReconciliationRunStatus.COMPLETED
              AND run.supersededAt IS NULL
            """)
    int supersedeWindow(
            @Param("merchantId") UUID merchantId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("currentRunId") UUID currentRunId,
            @Param("supersededAt") Instant supersededAt);
}
