package br.com.hanrry.reconpay.config;

import br.com.hanrry.reconpay.security.JwtAuthenticationFilter;
import br.com.hanrry.reconpay.security.RestAccessDeniedHandler;
import br.com.hanrry.reconpay.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String PATH_MERCHANT_TRANSACTIONS = "/api/merchants/*/transactions/**";
    private static final String PATH_EXTERNAL_SETTLEMENTS = "/api/merchants/*/external-settlements/**";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_FINANCIAL_ANALYST = "FINANCIAL_ANALYST";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, PATH_MERCHANT_TRANSACTIONS)
                        .hasAnyRole(ROLE_ADMIN, ROLE_FINANCIAL_ANALYST)
                        .requestMatchers(HttpMethod.POST, PATH_MERCHANT_TRANSACTIONS)
                        .hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, PATH_MERCHANT_TRANSACTIONS)
                        .hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, PATH_EXTERNAL_SETTLEMENTS)
                        .hasAnyRole(ROLE_ADMIN, ROLE_FINANCIAL_ANALYST)
                        .requestMatchers(HttpMethod.POST, PATH_EXTERNAL_SETTLEMENTS)
                        .hasRole(ROLE_ADMIN)
                        .requestMatchers("/api/users/**").hasRole(ROLE_ADMIN)
                        .requestMatchers("/api/merchants/**").hasRole(ROLE_ADMIN)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
