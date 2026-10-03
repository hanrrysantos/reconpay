package br.com.hanrry.reconpay.auth.email;

import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
@Profile("test")
@Primary
public class CapturingEmailSender implements EmailSender {

    private final AtomicReference<CapturedEmail> lastEmail = new AtomicReference<>();

    @Override
    public void sendEmailVerification(String toEmail, String recipientName, String rawToken) {
        lastEmail.set(new CapturedEmail(toEmail, recipientName, rawToken));
    }

    public CapturedEmail lastEmail() {
        return lastEmail.get();
    }

    public void clear() {
        lastEmail.set(null);
    }

    public record CapturedEmail(String toEmail, String recipientName, String rawToken) {
    }
}
