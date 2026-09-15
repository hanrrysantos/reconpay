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

    @Query("select r.id from ReconciliationRunEntity r where r.status = 'PENDING' order by r.createdAt, r.id")
    java.util.List<UUID> findPendingIds(Pageable pageable);

    @Modifying
    @Query("update ReconciliationRunEntity r set r.status = 'RUNNING', r.startedAt = :now where r.id = :id and r.status = 'PENDING'")
    int claimPending(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("update ReconciliationRunEntity r set r.status = 'PENDING', r.startedAt = null where r.id = :id and r.status = 'RUNNING'")
    int requeueRunning(@Param("id") UUID id);

    @Modifying
    @Query("update ReconciliationRunEntity r set r.status = 'PENDING', r.startedAt = null where r.status = 'RUNNING'")
    int recoverRunning();

    @Modifying
    @Query("update ReconciliationRunEntity r set r.status = 'FAILED', r.finishedAt = :now, r.errorMessage = :reason where r.id = :id and r.status = 'RUNNING'")
    int failRunning(@Param("id") UUID id, @Param("now") Instant now, @Param("reason") String reason);

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
