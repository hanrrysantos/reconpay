package br.com.hanrry.reconpay.reconciliation.repository;

import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
