package br.com.hanrry.reconpay.reconciliation.repository;

import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IReconciliationItemRepository extends
        JpaRepository<ReconciliationItemEntity, UUID>,
        JpaSpecificationExecutor<ReconciliationItemEntity> {

    @Query("""
            SELECT DISTINCT i FROM ReconciliationItemEntity i
            LEFT JOIN FETCH i.discrepancies
            LEFT JOIN FETCH i.internalTransaction
            LEFT JOIN FETCH i.externalSettlement
            WHERE i.reconciliationRun.id = :runId
            ORDER BY i.externalReference ASC
            """)
    List<ReconciliationItemEntity> findAllWithDetailsByRunId(@Param("runId") UUID runId);
}
