package br.com.hanrry.reconpay.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesBindingTest {

    private static final Map<String, Object> APPLICATION_YAML = Map.of(
            "reconpay.cors.allowed-origins[0]", "http://localhost:5173",
            "reconpay.cors.allowed-origins[1]", "http://127.0.0.1:5173",
            "reconpay.cors.allowed-origins[2]", "http://localhost:3000",
            "reconpay.cors.allowed-origins[3]", "http://127.0.0.1:3000");

    private static CorsProperties bind(Map<String, Object> environment) {
        MutablePropertySources sources = new MutablePropertySources();
        sources.addLast(new SystemEnvironmentPropertySource("systemEnvironment", environment));
        sources.addLast(new MapPropertySource("application.yaml", APPLICATION_YAML));
        return new Binder(ConfigurationPropertySources.from(sources))
                .bind("reconpay.cors", Bindable.of(CorsProperties.class))
                .get();
    }

    @Test
    void environmentVariableReplacesTheWholeYamlList() {
        CorsProperties properties = bind(Map.of(
                "RECONPAY_CORS_ALLOWED_ORIGINS", "https://reconpay.vercel.app,https://app.hanrry.top"));

        assertThat(properties.allowedOrigins()).containsExactly("https://reconpay.vercel.app", "https://app.hanrry.top");
    }

    @Test
    void environmentVariableWithASingleOriginKeepsOnlyIt() {
        CorsProperties properties = bind(Map.of("RECONPAY_CORS_ALLOWED_ORIGINS", "https://reconpay.vercel.app"));

        assertThat(properties.allowedOrigins()).containsExactly("https://reconpay.vercel.app");
    }

    @Test
    void spacesAroundOriginsAreDropped() {
        CorsProperties properties = bind(Map.of(
                "RECONPAY_CORS_ALLOWED_ORIGINS", " https://reconpay.vercel.app , https://app.hanrry.top "));

        assertThat(properties.allowedOrigins()).containsExactly("https://reconpay.vercel.app", "https://app.hanrry.top");
    }

    @Test
    void withoutTheVariableTheYamlOriginsStay() {
        CorsProperties properties = bind(Map.of());

        assertThat(properties.allowedOrigins()).containsExactly(
                "http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:3000", "http://127.0.0.1:3000");
    }
}
