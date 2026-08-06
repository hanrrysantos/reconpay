package br.com.hanrry.reconpay.security;

import br.com.hanrry.reconpay.exception.standardexceptionerror.ApiErrorCode;
import br.com.hanrry.reconpay.exception.standardexceptionerror.StandardError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import org.springframework.http.HttpStatus;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        writeError(response, request, HttpServletResponse.SC_UNAUTHORIZED, ApiErrorCode.UNAUTHORIZED,
                "Token inválido ou ausente", objectMapper);
    }

    static void writeError(
            HttpServletResponse response,
            HttpServletRequest request,
            int status,
            String errorCode,
            String message,
            ObjectMapper objectMapper
    ) throws IOException {
        StandardError body = StandardError.of(
                HttpStatus.valueOf(status),
                errorCode,
                message,
                request.getRequestURI(),
                null
        );

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
