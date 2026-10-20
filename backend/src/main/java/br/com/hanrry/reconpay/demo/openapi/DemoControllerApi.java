package br.com.hanrry.reconpay.demo.openapi;

import br.com.hanrry.reconpay.auth.dto.AuthResponseDTO;
import br.com.hanrry.reconpay.exception.standardexceptionerror.StandardError;
import br.com.hanrry.reconpay.openapi.OpenApiTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = OpenApiTags.AUTHENTICATION)
@SecurityRequirements
public interface DemoControllerApi {

    @Operation(
            summary = "Abrir conta de demonstração",
            description = """
                    Cria uma conta de demonstração temporária, sem cadastro, com uma empresa e dados \
                    de exemplo já conciliados, e retorna um token JWT Bearer. A conta expira em 24 horas \
                    e é apagada pela limpeza seguinte. Cada endereço cria até 2 demonstrações por hora, \
                    e no máximo 100 ficam ativas ao mesmo tempo. A demonstração permite até 2 empresas \
                    e 10 importações."""
    )
    @ApiResponse(
            responseCode = "201",
            description = "Demonstração criada; o token já dá acesso à empresa de exemplo",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AuthResponseDTO.class),
                    examples = @ExampleObject(
                            name = "token",
                            value = """
                                    {
                                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.exemplo",
                                      "type": "Bearer",
                                      "expiresIn": 86400
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "429",
            description = "Limite de demonstrações por endereço atingido na última hora",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StandardError.class),
                    examples = @ExampleObject(
                            name = "too-many-requests",
                            value = """
                                    {
                                      "timestamp": "2026-10-20T15:30:00Z",
                                      "status": 429,
                                      "error": "TOO_MANY_REQUESTS",
                                      "message": "Limite de demonstrações atingido. Tente novamente em até 1 hora.",
                                      "path": "/api/auth/demo"
                                    }
                                    """
                    )
            )
    )
    @ApiResponse(
            responseCode = "503",
            description = "Demonstrações esgotadas: já existem 100 ativas",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StandardError.class),
                    examples = @ExampleObject(
                            name = "service-unavailable",
                            value = """
                                    {
                                      "timestamp": "2026-10-20T15:30:00Z",
                                      "status": 503,
                                      "error": "SERVICE_UNAVAILABLE",
                                      "message": "Demonstrações esgotadas no momento. Tente novamente mais tarde.",
                                      "path": "/api/auth/demo"
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/api/auth/demo")
    ResponseEntity<AuthResponseDTO> create(@Parameter(hidden = true) HttpServletRequest request);
}
