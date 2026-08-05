package br.com.hanrry.reconpay.auth.mapper;

import br.com.hanrry.reconpay.auth.dto.UserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IUserMapperTest {

    private IUserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new IUserMapperImpl();
    }

    @Test
    void toDTOShouldMapAllResponseFieldsExceptPassword() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-05T12:00:00Z");

        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setName("Analista");
        entity.setEmail("analista@test.local");
        entity.setPassword("encoded-secret");
        entity.setRole(UserRole.FINANCIAL_ANALYST);
        entity.setActive(true);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);

        UserResponseDTO dto = mapper.toDTO(entity);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.name()).isEqualTo("Analista");
        assertThat(dto.email()).isEqualTo("analista@test.local");
        assertThat(dto.role()).isEqualTo(UserRole.FINANCIAL_ANALYST);
        assertThat(dto.active()).isTrue();
        assertThat(dto.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void toEntityShouldMapRequestFieldsAndIgnoreManagedFields() {
        UserRequestDTO request = new UserRequestDTO(
                "Novo Usuario",
                "novo@test.local",
                "Senha@123"
        );

        UserEntity entity = mapper.toEntity(request);

        assertThat(entity.getName()).isEqualTo("Novo Usuario");
        assertThat(entity.getEmail()).isEqualTo("novo@test.local");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getPassword()).isNull();
        assertThat(entity.getRole()).isNull();
        assertThat(entity.isActive()).isFalse();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }
}
