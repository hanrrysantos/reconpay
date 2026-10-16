package br.com.hanrry.reconpay.transaction.repository;

import br.com.hanrry.reconpay.transaction.entity.TransactionImportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ITransactionImportRepository extends JpaRepository<TransactionImportEntity, UUID> {

    Page<TransactionImportEntity> findAllByMerchant_Id(UUID merchantId, Pageable pageable);

    Optional<TransactionImportEntity> findByIdAndMerchant_Id(UUID id, UUID merchantId);
}
