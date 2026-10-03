package br.com.hanrry.reconpay.auth.service;

import br.com.hanrry.reconpay.auth.config.EmailProperties;
import br.com.hanrry.reconpay.auth.email.EmailSender;
import br.com.hanrry.reconpay.auth.entity.EmailVerificationTokenEntity;
import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.repository.IEmailVerificationTokenRepository;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.exception.InvalidEmailVerificationTokenException;
import br.com.hanrry.reconpay.exception.UserNotFoundException;
import br.com.hanrry.reconpay.observability.AuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final IEmailVerificationTokenRepository tokenRepository;
    private final IUserRepository userRepository;
    private final EmailSender emailSender;
    private final EmailVerificationTokenHasher tokenHasher;
    private final EmailProperties emailProperties;
    private final AuditLogger auditLogger;

    @Transactional
    public void sendVerificationEmail(UserEntity user) {
        tokenRepository.deleteAllPendingByUserId(user.getId());

        String rawToken = tokenHasher.generateRawToken();
        Instant expiresAt = Instant.now().plusSeconds(emailProperties.verificationTokenHours() * 3600);
        tokenRepository.save(new EmailVerificationTokenEntity(user.getId(), tokenHasher.hash(rawToken), expiresAt));

        emailSender.sendEmailVerification(user.getEmail(), user.getName(), rawToken);
    }

    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidEmailVerificationTokenException("Token de verificação inválido ou expirado");
        }

        EmailVerificationTokenEntity token = tokenRepository
                .findByTokenHashAndConsumedAtIsNull(tokenHasher.hash(rawToken.trim()))
                .orElseThrow(() -> new InvalidEmailVerificationTokenException("Token de verificação inválido ou expirado"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidEmailVerificationTokenException("Token de verificação inválido ou expirado");
        }

        UserEntity user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com id: " + token.getUserId()));

        user.setActive(true);
        userRepository.save(user);

        token.setConsumedAt(Instant.now());
        tokenRepository.save(token);

        auditLogger.record("USER_EMAIL_VERIFIED", "user", user.getId());
    }
}
