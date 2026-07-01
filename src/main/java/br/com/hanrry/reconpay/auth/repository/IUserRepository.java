package br.com.hanrry.reconpay.auth.repository;

import br.com.hanrry.reconpay.auth.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUserRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByEmail(String email);

    List<UserEntity> findAllByActiveTrue();

    Optional<UserEntity> findByIdAndActiveTrue(UUID id);

    Optional<UserEntity> findByEmailAndActiveTrue(String email);
}
