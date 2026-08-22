package br.com.hanrry.reconpay.auth.service;

import br.com.hanrry.reconpay.auth.dto.CreateUserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UpdateUserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.auth.mapper.IUserMapper;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.exception.EmailAlreadyExistsException;
import br.com.hanrry.reconpay.exception.UserNotFoundException;
import br.com.hanrry.reconpay.observability.AuditLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IUserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserShouldPersistUserWithRequestedRole() {
        CreateUserRequestDTO request = new CreateUserRequestDTO(
                "Novo Admin",
                "admin@test.local",
                "Admin@456",
                UserRole.ADMIN
        );

        UserEntity savedEntity = buildUserEntity(UserRole.ADMIN);
        UserResponseDTO expectedResponse = toResponseDTO(savedEntity);

        when(userRepository.existsByEmail("admin@test.local")).thenReturn(false);
        when(passwordEncoder.encode("Admin@456")).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);
        when(userMapper.toDTO(savedEntity)).thenReturn(expectedResponse);

        UserResponseDTO response = userService.createUser(request);

        ArgumentCaptor<UserEntity> entityCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(entityCaptor.capture());

        UserEntity captured = entityCaptor.getValue();
        assertThat(captured.getName()).isEqualTo("Novo Admin");
        assertThat(captured.getEmail()).isEqualTo("admin@test.local");
        assertThat(captured.getPassword()).isEqualTo("encoded-password");
        assertThat(captured.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void createUserShouldThrowWhenEmailAlreadyExists() {
        CreateUserRequestDTO request = new CreateUserRequestDTO(
                "Novo Admin",
                "admin@test.local",
                "Admin@456",
                UserRole.ADMIN
        );
        when(userRepository.existsByEmail("admin@test.local")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email já cadastrado: admin@test.local");

        verify(userRepository, never()).save(any());
    }

    @Test
    void findAllUsersShouldReturnOnlyActiveUsersMappedToDTO() {
        UserEntity activeUser = buildUserEntity(UserRole.ADMIN);
        UserResponseDTO responseDTO = toResponseDTO(activeUser);
        Page<UserEntity> page = new PageImpl<>(List.of(activeUser), PageRequest.of(0, 20), 1);

        when(userRepository.findAllByActiveTrue(PageRequest.of(0, 20))).thenReturn(page);
        when(userMapper.toDTO(activeUser)).thenReturn(responseDTO);

        Page<UserResponseDTO> result = userService.findAllUsers(PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(responseDTO);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findByIdShouldReturnUserWhenActive() {
        UUID id = UUID.randomUUID();
        UserEntity user = buildUserEntity(UserRole.ADMIN);
        user.setId(id);
        UserResponseDTO responseDTO = toResponseDTO(user);

        when(userRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(responseDTO);

        UserResponseDTO result = userService.findById(id);

        assertThat(result).isEqualTo(responseDTO);
    }

    @Test
    void findByIdShouldThrowWhenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(id))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Usuário não encontrado com id: " + id);
    }

    @Test
    void findByEmailShouldReturnUserWhenActive() {
        UserEntity user = buildUserEntity(UserRole.FINANCIAL_ANALYST);
        UserResponseDTO responseDTO = toResponseDTO(user);

        when(userRepository.findByEmailAndActiveTrue("analista@test.local")).thenReturn(Optional.of(user));
        when(userMapper.toDTO(user)).thenReturn(responseDTO);

        UserResponseDTO result = userService.findByEmail("analista@test.local");

        assertThat(result).isEqualTo(responseDTO);
    }

    @Test
    void findByEmailShouldThrowWhenUserNotFound() {
        when(userRepository.findByEmailAndActiveTrue("missing@test.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("missing@test.local"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Usuário não encontrado com email: missing@test.local");
    }

    @Test
    void updateNameShouldPersistNewName() {
        UUID id = UUID.randomUUID();
        UserEntity user = buildUserEntity(UserRole.ADMIN);
        user.setId(id);
        user.setName("Nome Antigo");

        UserEntity savedUser = buildUserEntity(UserRole.ADMIN);
        savedUser.setId(id);
        savedUser.setName("Nome Novo");
        UserResponseDTO responseDTO = toResponseDTO(savedUser);

        when(userRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(savedUser);
        when(userMapper.toDTO(savedUser)).thenReturn(responseDTO);

        UserResponseDTO result = userService.updateName(id, new UpdateUserRequestDTO("Nome Novo"));

        assertThat(user.getName()).isEqualTo("Nome Novo");
        assertThat(result).isEqualTo(responseDTO);
    }

    @Test
    void updateNameShouldThrowWhenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateName(id, new UpdateUserRequestDTO("Nome Novo")))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Usuário não encontrado com id: " + id);

        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteByIdShouldSoftDeleteUser() {
        UUID id = UUID.randomUUID();
        UserEntity user = buildUserEntity(UserRole.ADMIN);
        user.setId(id);
        user.setActive(true);

        when(userRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(user));

        userService.deleteById(id);

        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void deleteByIdShouldThrowWhenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteById(id))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Usuário não encontrado com id: " + id);

        verify(userRepository, never()).save(any());
    }

    private UserEntity buildUserEntity(UserRole role) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setName("Usuario");
        user.setEmail("usuario@test.local");
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(Instant.parse("2026-08-05T12:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-08-05T12:00:00Z"));
        return user;
    }

    private UserResponseDTO toResponseDTO(UserEntity user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
