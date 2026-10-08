package br.com.hanrry.reconpay.openapi;

import br.com.hanrry.reconpay.exception.standardexceptionerror.StandardError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(
        responseCode = "401",
        description = "Não autenticado: token Bearer ausente ou inválido",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StandardError.class),
                examples = @ExampleObject(
                        name = "unauthenticated",
                        value = """
                                {
                                  "timestamp": "2026-08-10T15:30:00Z",
                                  "status": 401,
                                  "error": "UNAUTHORIZED",
                                  "message": "Token ausente ou inválido",
                                  "path": "/api/me"
                                }
                                """
                )
        )
)
public @interface ApiUnauthenticatedResponse {
}
