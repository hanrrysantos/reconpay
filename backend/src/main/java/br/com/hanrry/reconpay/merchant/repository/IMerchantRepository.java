package br.com.hanrry.reconpay.merchant.repository;

import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IMerchantRepository extends JpaRepository<MerchantEntity, UUID> {

    boolean existsByDocument(String document);

    Page<MerchantEntity> findAllByActiveTrue(Pageable pageable);

    Page<MerchantEntity> findAllByActiveTrueAndIdIn(Collection<UUID> ids, Pageable pageable);

    Optional<MerchantEntity> findByIdAndActiveTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Transactional
    @Query("""
            select merchant
            from MerchantEntity merchant
            where merchant.id = :id
              and merchant.active = true
            """)
    Optional<MerchantEntity> findByIdAndActiveTrueForUpdate(@Param("id") UUID id);
}
