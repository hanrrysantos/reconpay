package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationConfigurationTest {
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ReconciliationProperties.class)
    static class Config { }

    @ParameterizedTest @ValueSource(strings = {"amount-tolerance=-0.01", "settlement-lag-days=-1", "max-window-days=0", "workers=0", "queue-capacity=-1"})
    void invalidOperationalLimitsPreventStartup(String property) {
        new ApplicationContextRunner().withUserConfiguration(Config.class)
                .withPropertyValues("reconpay.reconciliation." + property)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test void validBoundaryConfigurationStarts() {
        new ApplicationContextRunner().withUserConfiguration(Config.class)
                .withPropertyValues("reconpay.reconciliation.amount-tolerance=0", "reconpay.reconciliation.settlement-lag-days=0", "reconpay.reconciliation.queue-capacity=0")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test void baseConfigurationDoesNotActivateSeededDevelopment() {
        new ApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.config.location=classpath:application.yaml")
                .run(context -> assertThat(context.getEnvironment().getActiveProfiles()).doesNotContain("dev"));
    }
}
