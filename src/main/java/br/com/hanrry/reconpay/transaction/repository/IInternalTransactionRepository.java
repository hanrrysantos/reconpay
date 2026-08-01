package br.com.hanrry.reconpay.transaction.repository;

import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface IInternalTransactionRepository extends JpaRepository<InternalTransactionEntity, UUID>,
        JpaSpecificationExecutor<InternalTransactionEntity> {

    boolean existsByMerchant_IdAndExternalReference(UUID merchantId, String externalReference);

    Optional<InternalTransactionEntity> findByIdAndMerchant_Id(UUID id, UUID merchantId);
}
