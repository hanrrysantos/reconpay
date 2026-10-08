package br.com.hanrry.reconpay.auth.openapi;

import br.com.hanrry.reconpay.auth.dto.AuthRequestDTO;
import br.com.hanrry.reconpay.auth.dto.AuthResponseDTO;
import br.com.hanrry.reconpay.auth.dto.UserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.auth.dto.VerifyEmailRequestDTO;
import br.com.hanrry.reconpay.openapi.ApiConflictResponse;
import br.com.hanrry.reconpay.openapi.ApiUnauthorizedResponse;
import br.com.hanrry.reconpay.openapi.ApiValidationErrorResponse;
import br.com.hanrry.reconpay.openapi.OpenApiTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
        name = OpenApiTags.AUTHENTICATION,
        description = """
                Endpoints públicos de autenticação e cadastro de usuário.
                Não exigem token JWT. Use o token retornado no login no header \
                Authorization: Bearer {token} nas demais rotas protegidas."""
)
@SecurityRequirements
@RequestMapping("/api/auth")
public interface AuthControllerApi {

    @Operation(
            summary = "Autenticar usuário",
            description = """
                    Valida email e senha e retorna um token JWT Bearer.
                    O token expira em 24 horas e deve ser enviado \
                    nas requisições subsequentes."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Autenticação bem-sucedida",
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
    @ApiValidationErrorResponse
    @ApiUnauthorizedResponse
    @PostMapping("/login")
    ResponseEntity<AuthResponseDTO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "credenciais",
                                    value = """
                                            {
                                              "email": "admin@reconpay.local",
                                              "password": "DevAdmin@2026"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody AuthRequestDTO request);

    @Operation(
            summary = "Registrar novo usuário",
            description = """
                    Cria uma conta com perfil OPERATOR em estado inativo.
                    A senha deve ter no mínimo 8 caracteres, uma letra maiúscula e um número.
                    Um e-mail de verificação é enviado via Resend; use \
                    POST /api/auth/verify-email para ativar antes do login."""
    )
    @ApiResponse(
            responseCode = "201",
            description = "Usuário criado com sucesso, pendente de ativação",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseDTO.class),
                    examples = @ExampleObject(
                            name = "usuario-criado",
                            value = """
                                    {
                                      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "name": "Usuário Novo",
                                      "email": "usuario.novo@reconpay.local",
                                      "role": "OPERATOR",
                                      "active": false,
                                      "createdAt": "2026-08-10T12:00:00Z"
                                    }
                                    """
                    )
            )
    )
    @ApiValidationErrorResponse
    @ApiConflictResponse
    @PostMapping("/register")
    ResponseEntity<UserResponseDTO> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "cadastro",
                                    value = """
                                            {
                                              "name": "Usuário Novo",
                                              "email": "usuario.novo@reconpay.local",
                                              "password": "Usuario1"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UserRequestDTO request);

    @Operation(
            summary = "Confirmação de e-mail pela API",
            description = """
                    Ativa a conta com o token (JSON). Preferível para clientes programáticos;
                    usuários finais usam o link/botão do e-mail (GET)."""
    )
    @ApiResponse(responseCode = "204", description = "Confirmação concluída; a conta fica ativa")
    @ApiValidationErrorResponse
    @PostMapping("/verify-email")
    ResponseEntity<Void> verifyEmail(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = VerifyEmailRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "token",
                                    value = """
                                            {
                                              "token": "dGhpcy1pcy1hbi1leGFtcGxlLXRva2Vu"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody VerifyEmailRequestDTO request);

    @Operation(
            summary = "Confirmação de e-mail pelo link",
            description = """
                    Ativa a conta ao abrir o link do botão no e-mail de verificação.
                    Retorna uma página HTML de sucesso ou erro."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Página HTML de conta ativada",
            content = @Content(mediaType = MediaType.TEXT_HTML_VALUE)
    )
    @ApiResponse(
            responseCode = "400",
            description = "Página HTML de link inválido ou expirado",
            content = @Content(mediaType = MediaType.TEXT_HTML_VALUE)
    )
    @GetMapping(value = "/verify-email", produces = MediaType.TEXT_HTML_VALUE)
    ResponseEntity<String> verifyEmailFromLink(
            @Parameter(
                    name = "token",
                    description = "Token recebido no link do e-mail de verificação",
                    example = "dGhpcy1pcy1hbi1leGFtcGxlLXRva2Vu",
                    required = true
            )
            @RequestParam("token") String token);
}
