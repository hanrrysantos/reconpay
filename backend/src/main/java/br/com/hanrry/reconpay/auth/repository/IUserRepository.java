package br.com.hanrry.reconpay.auth.repository;

import br.com.hanrry.reconpay.auth.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IUserRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByEmail(String email);

    /**
     * A null {@code active} lists both states. Demo accounts are left out
     * unless {@code includeDemo} is true.
     */
    @Query("""
            select user from UserEntity user
            where (:active is null or user.active = :active)
              and (:includeDemo = true or user.demoExpiresAt is null)
            """)
    Page<UserEntity> findAllFiltered(
            @Param("active") Boolean active,
            @Param("includeDemo") boolean includeDemo,
            Pageable pageable);

    Optional<UserEntity> findByIdAndActiveTrue(UUID id);

    Optional<UserEntity> findByEmailAndActiveTrue(String email);

    Optional<UserEntity> findByEmail(String email);

    long countByDemoExpiresAtAfter(Instant instant);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") UUID id);
}
