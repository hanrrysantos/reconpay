package br.com.hanrry.reconpay.auth.email;

public interface EmailSender {

    void sendEmailVerification(String toEmail, String recipientName, String rawToken);
}
