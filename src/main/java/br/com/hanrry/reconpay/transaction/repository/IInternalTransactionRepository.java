package br.com.hanrry.reconpay.transaction.repository;

import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IInternalTransactionRepository extends JpaRepository<InternalTransactionEntity, UUID>,
        JpaSpecificationExecutor<InternalTransactionEntity> {

    boolean existsByMerchant_IdAndExternalReference(UUID merchantId, String externalReference);

    Optional<InternalTransactionEntity> findByIdAndMerchant_Id(UUID id, UUID merchantId);

    @Query("""
            SELECT transaction.externalReference FROM InternalTransactionEntity transaction
            WHERE transaction.merchant.id = :merchantId
              AND transaction.externalReference IN :references
            """)
    Set<String> findExistingReferences(
            @Param("merchantId") UUID merchantId,
            @Param("references") Collection<String> references);
}
