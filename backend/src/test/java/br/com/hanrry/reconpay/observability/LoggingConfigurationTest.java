package br.com.hanrry.reconpay.observability;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

class LoggingConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer());

    @Test
    void defaultProfileKeepsHumanReadableConsolePattern() {
        runner.withPropertyValues("spring.config.location=classpath:application.yaml")
                .run(context -> {
                    var env = context.getEnvironment();
                    assertThat(env.getProperty("logging.pattern.console"))
                            .contains("%X{requestId:-}")
                            .contains("%X{userEmail:-anonymous}");
                    assertThat(env.getProperty("logging.structured.format.console")).isNull();
                });
    }

    @Test
    void prodProfileEmitsLogstashJsonOnConsole() {
        runner.withPropertyValues(
                        "spring.config.location=classpath:application.yaml,classpath:application-prod.yaml",
                        "spring.profiles.active=prod")
                .run(context -> {
                    var env = context.getEnvironment();
                    assertThat(env.getProperty("logging.structured.format.console")).isEqualTo("logstash");
                    assertThat(env.getProperty("logging.structured.json.add.application")).isEqualTo("reconpay");
                });
    }
}
