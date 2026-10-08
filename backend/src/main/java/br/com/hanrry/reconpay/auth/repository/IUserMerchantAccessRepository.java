package br.com.hanrry.reconpay.auth.repository;

import br.com.hanrry.reconpay.auth.entity.UserMerchantAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IUserMerchantAccessRepository
        extends JpaRepository<UserMerchantAccessEntity, UserMerchantAccessEntity.UserMerchantId> {

    boolean existsByUserIdAndMerchantId(UUID userId, UUID merchantId);

    List<UserMerchantAccessEntity> findAllByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);
}
