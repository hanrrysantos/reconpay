package br.com.hanrry.reconpay.auth.openapi;

import br.com.hanrry.reconpay.auth.dto.AuthRequestDTO;
import br.com.hanrry.reconpay.auth.dto.AuthResponseDTO;
import br.com.hanrry.reconpay.auth.dto.UserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.openapi.ApiConflictResponse;
import br.com.hanrry.reconpay.openapi.ApiUnauthorizedResponse;
import br.com.hanrry.reconpay.openapi.ApiValidationErrorResponse;
import br.com.hanrry.reconpay.openapi.OpenApiTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(
        name = OpenApiTags.AUTHENTICATION,
        description = """
                Endpoints públicos de autenticação e auto-cadastro.
                Não exigem token JWT. Use o token retornado no login no header \
                `Authorization: Bearer {token}` nas demais rotas protegidas."""
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
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Autenticação bem-sucedida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponseDTO.class)
                    )
            )
    })
    @ApiValidationErrorResponse
    @ApiUnauthorizedResponse
    @PostMapping("/login")
    ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request);

    @Operation(
            summary = "Registrar novo usuário",
            description = """
                    Cria uma conta com perfil **FINANCIAL_ANALYST** por padrão.
                    A senha deve ter no mínimo 8 caracteres, uma letra maiúscula e um número.
                    Após o cadastro, use `/login` para obter o token JWT."""
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário criado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserResponseDTO.class)
                    )
            )
    })
    @ApiValidationErrorResponse
    @ApiConflictResponse
    @PostMapping("/register")
    ResponseEntity<UserResponseDTO> register(@Valid @RequestBody UserRequestDTO request);
}
