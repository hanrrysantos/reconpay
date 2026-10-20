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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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

    private final EmailProperties emailProperties = new EmailProperties(null, "from@test", "http://localhost", 24L, null);

    private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUpService() {
        emailVerificationService = new EmailVerificationService(
                tokenRepository,
                userRepository,
                emailSender,
                tokenHasher,
                emailProperties,
                auditLogger,
                clock
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

    @Test
    void resendShouldSendNewEmailToPendingSelfRegisteredUserAfterCooldown() {
        UserEntity user = pendingUser("ana@lojacentro.com.br");
        when(userRepository.findByEmail("ana@lojacentro.com.br")).thenReturn(Optional.of(user));
        when(tokenRepository.existsByUserIdAndConsumedAtIsNotNull(user.getId())).thenReturn(false);
        when(tokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(tokenCreatedAt(user, NOW.minusSeconds(60))));
        when(tokenHasher.generateRawToken()).thenReturn("fresh");
        when(tokenHasher.hash("fresh")).thenReturn("fresh-hash");

        emailVerificationService.resend("ana@lojacentro.com.br");

        verify(tokenRepository).deleteAllPendingByUserId(user.getId());
        ArgumentCaptor<EmailVerificationTokenEntity> captor =
                ArgumentCaptor.forClass(EmailVerificationTokenEntity.class);
        verify(tokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(user.getId());
        assertThat(captor.getValue().getTokenHash()).isEqualTo("fresh-hash");
        verify(emailSender).sendEmailVerification("ana@lojacentro.com.br", "Ana", "fresh");
    }

    @Test
    void resendShouldFindTheAccountWhenEmailHasSurroundingSpaces() {
        UserEntity user = pendingUser("ana@lojacentro.com.br");
        when(userRepository.findByEmail("ana@lojacentro.com.br")).thenReturn(Optional.of(user));
        when(tokenRepository.existsByUserIdAndConsumedAtIsNotNull(user.getId())).thenReturn(false);
        when(tokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(tokenCreatedAt(user, NOW.minusSeconds(600))));
        when(tokenHasher.generateRawToken()).thenReturn("fresh");
        when(tokenHasher.hash("fresh")).thenReturn("fresh-hash");

        emailVerificationService.resend("  ana@lojacentro.com.br  ");

        verify(emailSender).sendEmailVerification("ana@lojacentro.com.br", "Ana", "fresh");
    }

    @Test
    void resendShouldNotSendWhenAccountDoesNotExist() {
        when(userRepository.findByEmail("ninguem@x.com")).thenReturn(Optional.empty());

        emailVerificationService.resend("ninguem@x.com");

        assertNothingSent();
    }

    @Test
    void resendShouldNotSendWhenAccountIsActive() {
        UserEntity user = pendingUser("ana@lojacentro.com.br");
        user.setActive(true);
        when(userRepository.findByEmail("ana@lojacentro.com.br")).thenReturn(Optional.of(user));
        // Every other condition allows a resend, so only the active check can stop it
        // (self-registered account activated by an ADMIN, with a pending token).
        lenient().when(tokenRepository.existsByUserIdAndConsumedAtIsNotNull(user.getId())).thenReturn(false);
        lenient().when(tokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(tokenCreatedAt(user, NOW.minusSeconds(600))));
        lenient().when(tokenHasher.generateRawToken()).thenReturn("fresh");
        lenient().when(tokenHasher.hash("fresh")).thenReturn("fresh-hash");

        emailVerificationService.resend("ana@lojacentro.com.br");

        assertNothingSent();
    }

    @Test
    void resendShouldNotSendWhenAccountNeverReceivedVerificationEmail() {
        UserEntity user = pendingUser("ana@lojacentro.com.br");
        when(userRepository.findByEmail("ana@lojacentro.com.br")).thenReturn(Optional.of(user));
        when(tokenRepository.existsByUserIdAndConsumedAtIsNotNull(user.getId())).thenReturn(false);
        when(tokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.empty());

        emailVerificationService.resend("ana@lojacentro.com.br");

        assertNothingSent();
    }

    @Test
    void resendShouldNotReactivateAccountDeactivatedAfterConfirmation() {
        UserEntity user = pendingUser("ana@lojacentro.com.br");
        when(userRepository.findByEmail("ana@lojacentro.com.br")).thenReturn(Optional.of(user));
        when(tokenRepository.existsByUserIdAndConsumedAtIsNotNull(user.getId())).thenReturn(true);

        emailVerificationService.resend("ana@lojacentro.com.br");

        assertNothingSent();
    }

    @Test
    void resendShouldNotSendWithinSixtySecondsOfTheLastEmail() {
        UserEntity user = pendingUser("ana@lojacentro.com.br");
        when(userRepository.findByEmail("ana@lojacentro.com.br")).thenReturn(Optional.of(user));
        when(tokenRepository.existsByUserIdAndConsumedAtIsNotNull(user.getId())).thenReturn(false);
        when(tokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(Optional.of(tokenCreatedAt(user, NOW.minusSeconds(59))));

        emailVerificationService.resend("ana@lojacentro.com.br");

        assertNothingSent();
    }

    private static UserEntity pendingUser(String email) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setName("Ana");
        user.setActive(false);
        return user;
    }

    private static EmailVerificationTokenEntity tokenCreatedAt(UserEntity user, Instant createdAt) {
        EmailVerificationTokenEntity token = new EmailVerificationTokenEntity(user.getId(), "old-hash", createdAt.plusSeconds(86400));
        token.setCreatedAt(createdAt);
        return token;
    }

    private void assertNothingSent() {
        verify(emailSender, never()).sendEmailVerification(anyString(), anyString(), anyString());
        verify(tokenRepository, never()).deleteAllPendingByUserId(any());
        verify(tokenRepository, never()).save(any());
    }
}
