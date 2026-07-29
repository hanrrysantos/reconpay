package br.com.hanrry.reconpay.merchant.repository;

import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IMerchantRepository extends JpaRepository<MerchantEntity, UUID> {

    boolean existsByDocument(String document);

    Page<MerchantEntity> findAllByActiveTrue(Pageable pageable);

    Optional<MerchantEntity> findByIdAndActiveTrue(UUID id);
}
