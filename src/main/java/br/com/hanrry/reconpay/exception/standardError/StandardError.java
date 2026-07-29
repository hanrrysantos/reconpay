package br.com.hanrry.reconpay.exception.standardError;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record StandardError(

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path
) {
}
