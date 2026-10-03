package br.com.hanrry.reconpay.auth.email;

import br.com.hanrry.reconpay.auth.config.EmailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class EmailVerificationLinkFactory {

    private final EmailProperties emailProperties;

    public String buildConfirmUrl(String rawToken) {
        String base = emailProperties.verificationBaseUrl().replaceAll("/+$", "");
        return UriComponentsBuilder.fromHttpUrl(base)
                .path("/api/auth/verify-email")
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
    }
}
