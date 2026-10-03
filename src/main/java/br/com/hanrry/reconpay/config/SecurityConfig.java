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
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String PATH_MERCHANT_TRANSACTIONS = "/api/merchants/*/transactions/**";
    private static final String PATH_EXTERNAL_SETTLEMENTS = "/api/merchants/*/external-settlements/**";
    private static final String PATH_RECONCILIATIONS = "/api/merchants/*/reconciliations/**";
    private static final String PATH_FEE_RULES = "/api/merchants/*/fee-rules/**";
    private static final String PATH_MERCHANTS = "/api/merchants";
    private static final String PATH_MERCHANT_BY_ID = "/api/merchants/*";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_OPERATOR = "OPERATOR";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

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
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/verify-email").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/verify-email").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/api/users/**").hasRole(ROLE_ADMIN)
                        .requestMatchers(PATH_FEE_RULES).hasAnyRole(ROLE_ADMIN, ROLE_OPERATOR)
                        .requestMatchers(PATH_MERCHANT_TRANSACTIONS)
                        .hasAnyRole(ROLE_ADMIN, ROLE_OPERATOR)
                        .requestMatchers(PATH_EXTERNAL_SETTLEMENTS)
                        .hasAnyRole(ROLE_ADMIN, ROLE_OPERATOR)
                        .requestMatchers(PATH_RECONCILIATIONS)
                        .hasAnyRole(ROLE_ADMIN, ROLE_OPERATOR)
                        .requestMatchers(PATH_MERCHANTS, PATH_MERCHANT_BY_ID)
                        .hasAnyRole(ROLE_ADMIN, ROLE_OPERATOR)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
