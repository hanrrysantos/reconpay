package br.com.hanrry.reconpay.auth.email;

import br.com.hanrry.reconpay.auth.config.EmailProperties;
import br.com.hanrry.reconpay.auth.email.VerificationEmailComposer.VerificationEmail;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationEmailComposerTest {

    private static final String LINK = "https://app.example.com/verificar-email?token=q7Xk2mV9pL4r";

    private static VerificationEmailComposer composer(Long tokenHours) {
        return composer("https://app.example.com", tokenHours);
    }

    private static VerificationEmailComposer composer(String baseUrl, Long tokenHours) {
        EmailProperties properties = new EmailProperties(null, null, baseUrl, tokenHours, null);
        return new VerificationEmailComposer(new EmailVerificationLinkFactory(properties), properties);
    }

    @Test
    void htmlCarriesEveryPartOfTheDesign() {
        VerificationEmail email = composer(null).compose("ana@lojacentro.com.br", "Ana", "q7Xk2mV9pL4r");

        assertThat(email.subject()).isEqualTo("Confirme seu e-mail — ReconPay");
        assertThat(email.html())
                .contains("Olá, Ana!")
                .contains(">Confirme seu <span style=\"white-space:nowrap;\">e-mail</span></h1>")
                .contains("Falta um passo para ativar sua conta no ReconPay. Confirme que ana@lojacentro.com.br é seu e entre para cadastrar a primeira empresa.")
                .contains("<a href=\"" + LINK + "\"")
                .contains(">Confirmar e-mail</a>")
                .contains("O link vale por 24 horas e só pode ser usado uma vez.")
                .contains("Se o botão não funcionar, copie e cole este endereço no navegador:")
                .contains(">" + LINK + "<")
                .contains("Não criou uma conta no ReconPay? Ignore este e-mail: nada será ativado.");
    }

    @Test
    void headerShowsTheBrandMarkFromTheAppFollowedByTheName() {
        VerificationEmail email = composer(null).compose("ana@lojacentro.com.br", "Ana", "t");

        assertThat(email.html())
                .contains("<img src=\"https://app.example.com/brand/reconpay-app-icon.png\" width=\"32\" height=\"32\" alt=\"\" style=\"display:inline-block;width:32px;height:32px;")
                .contains("color:#FFFFFF;font-size:19px;font-weight:800;letter-spacing:-0.6px;vertical-align:middle;\">Recon<span style=\"color:#C6F135;\">Pay</span></span>")
                .doesNotContain("✓")
                .doesNotContain(">reconpay<");
    }

    @Test
    void brandMarkUrlIgnoresTrailingSlashesInTheBaseUrl() {
        VerificationEmail email = composer("https://app.example.com/", null).compose("ana@lojacentro.com.br", "Ana", "t");

        assertThat(email.html())
                .contains("<img src=\"https://app.example.com/brand/reconpay-app-icon.png\"")
                .doesNotContain("app.example.com//");
    }

    @Test
    void expiryFollowsTheConfiguredTokenHours() {
        VerificationEmail oneHour = composer(1L).compose("ana@lojacentro.com.br", "Ana", "t");
        VerificationEmail twoDays = composer(48L).compose("ana@lojacentro.com.br", "Ana", "t");

        assertThat(oneHour.html()).contains("O link vale por 1 hora e só pode ser usado uma vez.");
        assertThat(oneHour.text()).contains("O link vale por 1 hora e só pode ser usado uma vez.");
        assertThat(twoDays.html()).contains("O link vale por 48 horas e só pode ser usado uma vez.");
        assertThat(twoDays.text()).contains("O link vale por 48 horas e só pode ser usado uma vez.");
    }

    @Test
    void plainTextHasGreetingInstructionLinkExpiryAndIgnoreNotice() {
        VerificationEmail email = composer(null).compose("ana@lojacentro.com.br", "Ana", "q7Xk2mV9pL4r");

        assertThat(email.text())
                .contains("Olá, Ana!")
                .contains("Falta um passo para ativar sua conta no ReconPay. Confirme que ana@lojacentro.com.br é seu abrindo o endereço abaixo:")
                .contains("\n" + LINK + "\n")
                .contains("O link vale por 24 horas e só pode ser usado uma vez.")
                .contains("Não criou uma conta no ReconPay? Ignore este e-mail: nada será ativado.")
                .contains("ReconPay · Conciliação financeira")
                .doesNotContain("reconpay")
                .doesNotContain("<");
    }

    @Test
    void htmlEscapesNameAndEmail() {
        VerificationEmail email = composer(null).compose("a&b@x.com", "<b>\"Zé\" & 'Ana'</b>", "t");

        assertThat(email.html())
                .contains("Olá, &lt;b&gt;&quot;Zé&quot; &amp; &#39;Ana&#39;&lt;/b&gt;!")
                .contains("Confirme que a&amp;b@x.com é seu")
                .doesNotContain("<b>")
                .doesNotContain("\"Zé\"");
        assertThat(email.text()).contains("Olá, <b>\"Zé\" & 'Ana'</b>!");
    }
}
