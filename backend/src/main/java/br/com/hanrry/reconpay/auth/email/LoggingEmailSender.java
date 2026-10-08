package br.com.hanrry.reconpay.auth.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(EmailSender.class)
public class LoggingEmailSender implements EmailSender {

    @Override
    public void sendEmailVerification(String toEmail, String recipientName, String rawToken) {
        log.warn("""
                reconpay.email.resend-api-key not set — verification email not sent.
                to={} name={} token={}
                """, toEmail, recipientName, rawToken);
    }
}
