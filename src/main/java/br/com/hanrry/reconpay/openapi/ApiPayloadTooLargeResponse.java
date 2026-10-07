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
        responseCode = "413",
        description = "Arquivo CSV acima do limite de 5MB",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StandardError.class),
                examples = @ExampleObject(
                        name = "payload-too-large",
                        value = """
                                {
                                  "timestamp": "2026-08-10T15:30:00Z",
                                  "status": 413,
                                  "error": "VALIDATION_ERROR",
                                  "message": "Arquivo CSV excede o tamanho máximo permitido de 5MB",
                                  "path": "/api/merchants/3fa85f64-5717-4562-b3fc-2c963f66afa6/external-settlements/import"
                                }
                                """
                )
        )
)
public @interface ApiPayloadTooLargeResponse {
}
