package br.com.hanrry.reconpay.config;

import br.com.hanrry.reconpay.openapi.OpenApiSecuritySchemes;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI reconPayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ReconPay API")
                        .description("""
                                O Reconpay permite registrar transações, importar arquivos de liquidação, \
                                executar conciliação por estabelecimentos e analisar divergências \
                                (valores, referências ausentes, duplicidades e inconsistências de taxa).

                                O objetivo é dar visibilidade e controle ao time financeiro sobre o que \
                                foi vendido, o que foi liquidado e o que ainda precisa de tratativa.

                                ## Papéis
                                - **ADMIN**: governança (`/api/users/**`) e bypass de escopo por merchant.
                                - **OPERATOR**: fluxo operacional nos merchants concedidos (ou auto-grant ao criar).

                                ## Fluxo de teste recomendado (seed dev)
                                1. POST /api/auth/login com usuário ADMIN ou OPERATOR do README.
                                2. Authorization: Bearer {token} nas rotas protegidas.
                                3. GET /api/me e GET /api/me/merchants para validar contexto.

                                ## Auto-cadastro
                                1. POST /api/auth/register → conta OPERATOR inativa + e-mail de verificação.
                                2. POST /api/auth/verify-email com o token recebido.
                                3. POST /api/auth/login.

                                **Configuração**
                                1. POST /api/merchants (criador recebe grant)
                                2. POST /api/merchants/{merchantId}/fee-rules

                                **Dados e conciliação**
                                3. POST .../transactions · 4. POST .../external-settlements/import
                                5. POST .../reconciliations · 6. GET .../items · 7. GET .../export
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Hanrry Santos: hanrry.jsantos@gmail.com")))
                .addSecurityItem(new SecurityRequirement().addList(OpenApiSecuritySchemes.BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(OpenApiSecuritySchemes.BEARER_AUTH, new SecurityScheme()
                                .name(OpenApiSecuritySchemes.BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtido via POST /api/auth/login")));
    }
}
