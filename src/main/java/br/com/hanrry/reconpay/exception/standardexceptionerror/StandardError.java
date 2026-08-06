package br.com.hanrry.reconpay.exception.standardexceptionerror;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

import java.time.Instant;

public record StandardError(

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path,
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
