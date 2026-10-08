package br.com.hanrry.reconpay.externalsettlement.repository;

import br.com.hanrry.reconpay.externalsettlement.entity.SettlementImportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ISettlementImportRepository extends JpaRepository<SettlementImportEntity, UUID> {

    Page<SettlementImportEntity> findAllByMerchant_Id(UUID merchantId, Pageable pageable);

    Optional<SettlementImportEntity> findByIdAndMerchant_Id(UUID id, UUID merchantId);
}
