package br.com.hanrry.reconpay.auth.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean(EmailSender.class)
public class LoggingEmailSender implements EmailSender {

    private final VerificationEmailComposer verificationEmailComposer;

    @Override
    public void sendEmailVerification(String toEmail, String recipientName, String rawToken) {
        VerificationEmailComposer.VerificationEmail email = verificationEmailComposer.compose(toEmail, recipientName, rawToken);
        log.warn("""
                reconpay.email.resend-api-key not set — verification email not sent.
                to={} subject={}
                {}""", toEmail, email.subject(), email.text());
    }
}
