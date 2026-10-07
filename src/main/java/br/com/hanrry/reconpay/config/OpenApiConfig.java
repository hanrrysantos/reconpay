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
                                O ReconPay registra o que foi vendido, o que foi liquidado e o que o extrato \
                                confirma. O time financeiro executa a conciliação por merchant e vê \
                                as divergências que ainda pedem tratativa.

                                ADMIN governa os usuários e acessa qualquer merchant. \
                                OPERATOR opera os merchants para os quais tem grant, inclusive o \
                                auto-grant de quem cria um merchant.
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
