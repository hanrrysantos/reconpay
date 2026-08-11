package br.com.hanrry.reconpay.exception.standardexceptionerror;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Schema(description = "Formato padrão de resposta de erro da API")
public record StandardError(

        @Schema(description = "Momento do erro (UTC)", example = "2026-08-10T15:30:00Z")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant timestamp,

        @Schema(description = "Código HTTP", example = "400")
        Integer status,

        @Schema(description = "Código de erro da aplicação", example = "VALIDATION_ERROR")
        String error,

        @Schema(description = "Mensagem descritiva do erro", example = "Email é obrigatório")
        String message,

        @Schema(description = "Caminho da requisição que gerou o erro", example = "/api/auth/login")
        String path,

        @Schema(description = "Detalhes adicionais do erro, quando aplicável")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Object details
) {

    public static StandardError of(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request
    ) {
        return of(status, error, message, request.getRequestURI(), null);
    }

    public static StandardError of(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request,
            Object details
    ) {
        return of(status, error, message, request.getRequestURI(), details);
    }

    public static StandardError of(
            HttpStatus status,
            String error,
            String message,
            String path,
            Object details
    ) {
        return new StandardError(
                Instant.now(),
                status.value(),
                error,
                message,
                path,
                details
        );
    }
}
