package br.com.hanrry.reconpay.auth.service;

import br.com.hanrry.reconpay.auth.dto.AuthRequestDTO;
import br.com.hanrry.reconpay.auth.dto.AuthResponseDTO;
import br.com.hanrry.reconpay.auth.dto.UserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.auth.mapper.IUserMapper;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.exception.EmailAlreadyExistsException;
import br.com.hanrry.reconpay.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IUserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginShouldAuthenticateAndGenerateToken() {
        AuthRequestDTO request = new AuthRequestDTO("analista@gmail.com", "Analista@123");
        when(jwtService.generateToken("analista@gmail.com")).thenReturn("jwt-token");

        AuthResponseDTO response = authService.login(request);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("analista@gmail.com", "Analista@123")
        );
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(86400);
    }

    @Test
    void registerShouldPersistUserWithEncodedPassword() {
        UserRequestDTO request = new UserRequestDTO("Analista", "analista@gmail.com", "Analista@123");
        UserEntity mappedEntity = new UserEntity();
        mappedEntity.setName("Analista");
        mappedEntity.setEmail("analista@gmail.com");

        UserEntity savedEntity = new UserEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setName("Analista");
        savedEntity.setEmail("analista@gmail.com");
        savedEntity.setPassword("encoded-password");
        savedEntity.setRole(UserRole.FINANCIAL_ANALYST);
        savedEntity.setActive(true);
        savedEntity.setCreatedAt(Instant.parse("2026-08-05T12:00:00Z"));

        UserResponseDTO expectedResponse = new UserResponseDTO(
                savedEntity.getId(),
                "Analista",
                "analista@gmail.com",
                UserRole.FINANCIAL_ANALYST,
                true,
                savedEntity.getCreatedAt()
        );

        when(userRepository.existsByEmail("analista@gmail.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(mappedEntity);
        when(passwordEncoder.encode("Analista@123")).thenReturn("encoded-password");

        when(userRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(userMapper.toDTO(savedEntity)).thenReturn(expectedResponse);

        UserResponseDTO response = authService.register(request);

        ArgumentCaptor<UserEntity> entityCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(entityCaptor.capture());

        assertThat(entityCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void registerShouldThrowWhenEmailAlreadyExists() {
        UserRequestDTO request = new UserRequestDTO("Analista", "analista@gmail.com", "Analista@123");
        when(userRepository.existsByEmail("analista@gmail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email já cadastrado: analista@gmail.com");

        verify(userMapper, never()).toEntity(any());
        verify(userRepository, never()).save(any());
    }
}
