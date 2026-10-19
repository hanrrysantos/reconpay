package br.com.hanrry.reconpay.auth.email;

import br.com.hanrry.reconpay.auth.config.EmailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class EmailVerificationLinkFactory {

    private static final String BRAND_MARK_PATH = "/brand/reconpay-mark-on-dark.png";

    private final EmailProperties emailProperties;

    public String buildConfirmUrl(String rawToken) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl())
                .path("/verificar-email")
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
    }

    /** The brand mark the email header shows, served by the frontend next to the verification page. */
    public String buildBrandMarkUrl() {
        return baseUrl() + BRAND_MARK_PATH;
    }

    private String baseUrl() {
        return emailProperties.verificationBaseUrl().replaceAll("/+$", "");
    }
}
