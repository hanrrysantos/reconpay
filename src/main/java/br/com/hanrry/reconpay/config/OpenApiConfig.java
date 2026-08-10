package br.com.hanrry.reconpay.config;

import br.com.hanrry.reconpay.openapi.common.OpenApiSecuritySchemes;
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

                                ## Fluxo de teste recomendado:
                                - O ciclo completo exige um usuário com perfil ADMIN.

                                - Inicie autenticando em POST /api/auth/login, usando o payload de exemplo \
                                já preenchido no endpoint Autenticar usuário (Authentication).

                                - Utilize o token retornado no header Authorization: Bearer {token} \
                                em todas as etapas seguintes.

                                **Configuração**
                                1. Cadastrar estabelecimento: POST /api/merchants
                                2. Definir regra de taxa: POST /api/merchants/{merchantId}/fee-rules

                                **Dados**
                                3. Registrar transações internas: \
                                POST /api/merchants/{merchantId}/transactions
                                4. Importar liquidação externa (CSV): \
                                POST /api/merchants/{merchantId}/external-settlements/import

                                **Conciliação**
                                5. Executar conciliação: POST /api/merchants/{merchantId}/reconciliations
                                6. Consultar divergências: \
                                GET /api/merchants/{merchantId}/reconciliations/{runId}/items
                                7. Exportar resultado (CSV): \
                                GET /api/merchants/{merchantId}/reconciliations/{runId}/export
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
