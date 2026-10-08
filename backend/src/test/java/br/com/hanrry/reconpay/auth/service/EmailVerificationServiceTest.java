package br.com.hanrry.reconpay.auth.service;

import br.com.hanrry.reconpay.auth.config.EmailProperties;
import br.com.hanrry.reconpay.auth.email.EmailSender;
import br.com.hanrry.reconpay.auth.entity.EmailVerificationTokenEntity;
import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.repository.IEmailVerificationTokenRepository;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.exception.InvalidEmailVerificationTokenException;
import br.com.hanrry.reconpay.observability.AuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private IEmailVerificationTokenRepository tokenRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private EmailSender emailSender;

    @Mock
    private EmailVerificationTokenHasher tokenHasher;

    @Mock
    private AuditLogger auditLogger;

    private final EmailProperties emailProperties = new EmailProperties(null, "from@test", "http://localhost", 24L);

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUpService() {
        emailVerificationService = new EmailVerificationService(
                tokenRepository,
                userRepository,
                emailSender,
                tokenHasher,
                emailProperties,
                auditLogger
        );
    }

    @Test
    void verifyShouldActivateUserAndConsumeToken() {
        UUID userId = UUID.randomUUID();
        String raw = "raw-token";
        String hash = "abc123";

        EmailVerificationTokenEntity tokenEntity = new EmailVerificationTokenEntity(userId, hash, Instant.now().plusSeconds(3600));
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setActive(false);

        when(tokenHasher.hash(raw)).thenReturn(hash);
        when(tokenRepository.findByTokenHashAndConsumedAtIsNull(hash)).thenReturn(Optional.of(tokenEntity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        emailVerificationService.verify(raw);

        assertThat(user.isActive()).isTrue();
        assertThat(tokenEntity.getConsumedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(tokenRepository).save(tokenEntity);
    }

    @Test
    void verifyShouldRejectUnknownToken() {
        when(tokenHasher.hash("bad")).thenReturn("hash");
        when(tokenRepository.findByTokenHashAndConsumedAtIsNull("hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verify("bad"))
                .isInstanceOf(InvalidEmailVerificationTokenException.class);
    }

    @Test
    void sendVerificationEmailShouldPersistTokenAndSendMail() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.local");
        user.setName("User");

        when(tokenHasher.generateRawToken()).thenReturn("generated");
        when(tokenHasher.hash("generated")).thenReturn("hashed");

        emailVerificationService.sendVerificationEmail(user);

        verify(tokenRepository).deleteAllPendingByUserId(user.getId());
        ArgumentCaptor<EmailVerificationTokenEntity> captor =
                ArgumentCaptor.forClass(EmailVerificationTokenEntity.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo("hashed");
        verify(emailSender).sendEmailVerification("user@test.local", "User", "generated");
    }
}
