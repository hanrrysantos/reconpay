package br.com.hanrry.reconpay.reconciliation.repository;

import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IReconciliationRunRepository extends JpaRepository<ReconciliationRunEntity, UUID> {

    Page<ReconciliationRunEntity> findAllByMerchant_Id(UUID merchantId, Pageable pageable);

    Optional<ReconciliationRunEntity> findByIdAndMerchant_Id(UUID id, UUID merchantId);
}
