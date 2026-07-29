package br.com.hanrry.reconpay.security;

import br.com.hanrry.reconpay.exception.standardError.ApiErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        RestAuthenticationEntryPoint.writeError(
                response,
                request,
                HttpServletResponse.SC_FORBIDDEN,
                ApiErrorCode.FORBIDDEN,
                "Acesso negado para este perfil",
                objectMapper
        );
    }
}
