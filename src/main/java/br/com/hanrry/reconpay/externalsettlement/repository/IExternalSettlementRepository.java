package br.com.hanrry.reconpay.externalsettlement.repository;

import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IExternalSettlementRepository extends
        JpaRepository<ExternalSettlementEntity, UUID>,
        JpaSpecificationExecutor<ExternalSettlementEntity> {

    Optional<ExternalSettlementEntity> findByIdAndMerchant_Id(UUID id, UUID merchantId);

    List<ExternalSettlementEntity> findByMerchant_IdAndExternalReferenceIn(
            UUID merchantId,
            Collection<String> externalReferences);
}
