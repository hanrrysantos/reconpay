package br.com.hanrry.reconpay.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailRedirectProfileTest {

    private static final Map<String, Object> ENV = Map.of("RECONPAY_EMAIL_REDIRECT_TO", "dev@example.com");

    /**
     * Binds reconpay.email from application.yaml plus the profile files, with the variable set the way the dev profile
     * imports it from ../.env. An OS environment variable binds in any profile; ResendEmailSender guards against that.
     */
    private static EmailProperties bind(String... profiles) throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("env", ENV));
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (String profile : profiles) {
            loader.load(profile, new ClassPathResource("application-" + profile + ".yaml"))
                    .forEach(source -> environment.getPropertySources().addBefore("env", source));
        }
        loader.load("base", new ClassPathResource("application.yaml"))
                .forEach(environment.getPropertySources()::addLast);
        return Binder.get(environment).bindOrCreate("reconpay.email", EmailProperties.class);
    }

    @Test
    void devProfileReadsTheRedirectVariable() throws IOException {
        assertThat(bind("dev").redirectTo()).isEqualTo("dev@example.com");
    }

    @Test
    void otherProfilesDoNotMapTheEnvFileVariable() throws IOException {
        for (List<String> profiles : List.of(List.<String>of(), List.of("test"), List.of("prod"))) {
            assertThat(bind(profiles.toArray(String[]::new)).redirectTo()).as("profiles %s", profiles).isNull();
        }
    }
}
