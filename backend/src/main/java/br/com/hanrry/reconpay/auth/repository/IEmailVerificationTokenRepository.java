package br.com.hanrry.reconpay.auth.repository;

import br.com.hanrry.reconpay.auth.entity.EmailVerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface IEmailVerificationTokenRepository extends JpaRepository<EmailVerificationTokenEntity, UUID> {

    Optional<EmailVerificationTokenEntity> findByTokenHashAndConsumedAtIsNull(String tokenHash);

    @Modifying
    @Query("DELETE FROM EmailVerificationTokenEntity t WHERE t.userId = :userId AND t.consumedAt IS NULL")
    void deleteAllPendingByUserId(@Param("userId") UUID userId);
}
