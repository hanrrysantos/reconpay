package br.com.hanrry.reconpay.auth.openapi;

import br.com.hanrry.reconpay.auth.dto.CreateUserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.MerchantAccessRequestDTO;
import br.com.hanrry.reconpay.auth.dto.MerchantAccessResponseDTO;
import br.com.hanrry.reconpay.auth.dto.UpdateUserRequestDTO;
import br.com.hanrry.reconpay.auth.dto.UserResponseDTO;
import br.com.hanrry.reconpay.openapi.ApiConflictResponse;
import br.com.hanrry.reconpay.openapi.ApiForbiddenResponse;
import br.com.hanrry.reconpay.openapi.ApiNotFoundResponse;
import br.com.hanrry.reconpay.openapi.ApiUnauthenticatedResponse;
import br.com.hanrry.reconpay.openapi.ApiValidationErrorResponse;
import br.com.hanrry.reconpay.openapi.OpenApiSecuritySchemes;
import br.com.hanrry.reconpay.openapi.OpenApiTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = OpenApiTags.USERS, description = "Administração de usuários, restrita ao papel ADMIN")
@SecurityRequirement(name = OpenApiSecuritySchemes.BEARER_AUTH)
@RequestMapping("/api/users")
public interface UserControllerApi {

    String USER_ID_EXAMPLE = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    @Operation(
            summary = "Criação de usuário",
            description = """
                    Cria uma conta já ativa, com o papel informado. \
                    Somente ADMIN executa esta operação. \
                    O id da resposta serve nas rotas seguintes."""
    )
    @ApiResponse(
            responseCode = "201",
            description = "Usuário criado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseDTO.class),
                    examples = @ExampleObject(name = "usuario", value = USER_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiConflictResponse
    @PostMapping
    ResponseEntity<UserResponseDTO> createUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateUserRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "novo-usuario",
                                    value = """
                                            {
                                              "name": "Ana Operadora",
                                              "email": "ana.operadora@reconpay.local",
                                              "password": "Operadora1",
                                              "role": "OPERATOR"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody CreateUserRequestDTO request);

    @Operation(
            summary = "Listagem de usuários",
            description = "Lista paginada dos usuários ativos. Somente ADMIN consulta esta coleção."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Página de usuários ativos",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(
                            name = "usuarios",
                            value = """
                                    {
                                      "content": [
                                        {
                                          "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                          "name": "Ana Operadora",
                                          "email": "ana.operadora@reconpay.local",
                                          "role": "OPERATOR",
                                          "active": true,
                                          "createdAt": "2026-08-10T12:00:00Z"
                                        }
                                      ],
                                      "totalElements": 1,
                                      "totalPages": 1,
                                      "size": 20,
                                      "number": 0
                                    }
                                    """
                    )
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @Parameters({
            @Parameter(name = "sort", in = ParameterIn.QUERY, example = "name,asc")
    })
    @GetMapping
    ResponseEntity<Page<UserResponseDTO>> findAllUsers(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable);

    @Operation(
            summary = "Consulta de usuário por identificador",
            description = """
                    Devolve um usuário ativo. O id vem da resposta de criação ou da listagem. \
                    Somente ADMIN consulta."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Usuário encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseDTO.class),
                    examples = @ExampleObject(name = "usuario", value = USER_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping("/{id}")
    ResponseEntity<UserResponseDTO> findById(@Parameter(description = "Identificador do usuário. O id vem da resposta de criação ou da listagem.", example = USER_ID_EXAMPLE) @PathVariable UUID id);

    @Operation(
            summary = "Consulta de usuário por e-mail",
            description = "Devolve o usuário ativo com este e-mail. Somente ADMIN consulta."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Usuário encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseDTO.class),
                    examples = @ExampleObject(name = "usuario", value = USER_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping("/email")
    ResponseEntity<UserResponseDTO> findByEmail(
            @Parameter(
                    name = "email",
                    description = "E-mail do usuário ativo",
                    example = "ana.operadora@reconpay.local",
                    required = true
            )
            @RequestParam String email);

    @Operation(
            summary = "Ativação da conta do usuário",
            description = """
                    Marca a conta como ativa. O id vem da resposta de criação ou da listagem. \
                    Somente ADMIN ativa."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Conta do usuário ativada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseDTO.class),
                    examples = @ExampleObject(name = "usuario", value = USER_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @PatchMapping("/{id}/activation")
    ResponseEntity<UserResponseDTO> activate(@Parameter(description = "Identificador do usuário. O id vem da resposta de criação ou da listagem.", example = USER_ID_EXAMPLE) @PathVariable UUID id);

    @Operation(
            summary = "Grants de merchant do usuário",
            description = """
                    Lista os grants de merchant do usuário. \
                    O id vem da resposta de criação ou da listagem. Somente ADMIN consulta."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Grants do usuário",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MerchantAccessResponseDTO.class),
                    examples = @ExampleObject(name = "grants", value = ACCESS_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping("/{id}/merchants")
    ResponseEntity<MerchantAccessResponseDTO> findMerchantAccess(@Parameter(description = "Identificador do usuário. O id vem da resposta de criação ou da listagem.", example = USER_ID_EXAMPLE) @PathVariable UUID id);

    @Operation(
            summary = "Substituição dos merchants do usuário",
            description = """
                    Substitui a lista de grants de merchant. \
                    O id vem da resposta de criação ou da listagem. Somente ADMIN altera."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Grants substituídos",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MerchantAccessResponseDTO.class),
                    examples = @ExampleObject(name = "grants", value = ACCESS_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @PutMapping("/{id}/merchants")
    ResponseEntity<MerchantAccessResponseDTO> replaceMerchantAccess(
            @Parameter(description = "Identificador do usuário. O id vem da resposta de criação ou da listagem.", example = USER_ID_EXAMPLE)
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MerchantAccessRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "grants",
                                    value = """
                                            {
                                              "merchantIds": ["3fa85f64-5717-4562-b3fc-2c963f66afa6"]
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody MerchantAccessRequestDTO request);

    @Operation(
            summary = "Atualização do nome do usuário",
            description = """
                    Altera o nome de um usuário ativo. \
                    O id vem da resposta de criação ou da listagem. Somente ADMIN altera."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Nome do usuário atualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = UserResponseDTO.class),
                    examples = @ExampleObject(name = "usuario", value = USER_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @PutMapping("/{id}")
    ResponseEntity<UserResponseDTO> updateName(
            @Parameter(description = "Identificador do usuário. O id vem da resposta de criação ou da listagem.", example = USER_ID_EXAMPLE)
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateUserRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "nome",
                                    value = """
                                            {
                                              "name": "Ana Operadora"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpdateUserRequestDTO request);

    @Operation(
            summary = "Exclusão do usuário",
            description = """
                    Desativa a conta. O id vem da resposta de criação ou da listagem. \
                    Somente ADMIN exclui."""
    )
    @ApiResponse(responseCode = "204", description = "Conta desativada; não há corpo na resposta")
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@Parameter(description = "Identificador do usuário. O id vem da resposta de criação ou da listagem.", example = USER_ID_EXAMPLE) @PathVariable UUID id);

    String USER_JSON = """
            {
              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "name": "Ana Operadora",
              "email": "ana.operadora@reconpay.local",
              "role": "OPERATOR",
              "active": true,
              "createdAt": "2026-08-10T12:00:00Z"
            }
            """;

    String ACCESS_JSON = """
            {
              "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "merchantIds": ["3fa85f64-5717-4562-b3fc-2c963f66afa6"]
            }
            """;
}
