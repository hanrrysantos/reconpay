package br.com.hanrry.reconpay.merchant.openapi;

import br.com.hanrry.reconpay.merchant.dto.MerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.dto.MerchantResponseDTO;
import br.com.hanrry.reconpay.merchant.dto.UpdateMerchantRequestDTO;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Tag(name = OpenApiTags.MERCHANTS, description = "Cadastro e manutenção dos merchants")
@SecurityRequirement(name = OpenApiSecuritySchemes.BEARER_AUTH)
@RequestMapping("/api/merchants")
public interface MerchantControllerApi {

    String MERCHANT_ID_EXAMPLE = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    String MERCHANT_JSON = """
            {
              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "name": "Loja Centro",
              "document": "12345678000199",
              "active": true,
              "createdAt": "2026-08-10T12:00:00Z"
            }
            """;

    @Operation(
            summary = "Criação de merchant",
            description = """
                    Cria o merchant e concede acesso ao criador (auto-grant). \
                    Depois da criação, o id da resposta serve nas rotas seguintes."""
    )
    @ApiResponse(
            responseCode = "201",
            description = "Merchant criado com êxito",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MerchantResponseDTO.class),
                    examples = @ExampleObject(name = "merchant", value = MERCHANT_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiConflictResponse
    @PostMapping
    ResponseEntity<MerchantResponseDTO> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MerchantRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "novo-merchant",
                                    value = """
                                            {
                                              "name": "Loja Centro",
                                              "document": "12345678000199"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody MerchantRequestDTO request);

    @Operation(
            summary = "Página de merchants",
            description = """
                    Lista paginada dos merchants ativos. \
                    OPERATOR vê os grants; ADMIN vê todos os ativos."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Página de merchants ativos",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(
                            name = "merchants",
                            value = """
                                    {
                                      "content": [
                                        {
                                          "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                          "name": "Loja Centro",
                                          "document": "12345678000199",
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
    ResponseEntity<Page<MerchantResponseDTO>> findAll(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable);

    @Operation(
            summary = "Consulta do merchant específico",
            description = "Devolve um merchant ativo. O id vem da resposta de criação ou da listagem."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Merchant encontrado com êxito",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MerchantResponseDTO.class),
                    examples = @ExampleObject(name = "merchant", value = MERCHANT_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping("/{id}")
    ResponseEntity<MerchantResponseDTO> findById(
            @Parameter(description = "Identificador do merchant. O id vem da resposta de criação ou da listagem.", example = MERCHANT_ID_EXAMPLE)
            @PathVariable UUID id);

    @Operation(
            summary = "Atualização do merchant",
            description = "Altera o nome de um merchant ativo. O id vem da resposta de criação ou da listagem."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Merchant atualizado com êxito",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MerchantResponseDTO.class),
                    examples = @ExampleObject(name = "merchant", value = MERCHANT_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @PutMapping("/{id}")
    ResponseEntity<MerchantResponseDTO> update(
            @Parameter(description = "Identificador do merchant. O id vem da resposta de criação ou da listagem.", example = MERCHANT_ID_EXAMPLE)
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateMerchantRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "nome",
                                    value = """
                                            {
                                              "name": "Loja Centro Atualizada"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpdateMerchantRequestDTO request);

    @Operation(
            summary = "Exclusão do merchant",
            description = "Desativa o merchant. O id vem da resposta de criação ou da listagem."
    )
    @ApiResponse(responseCode = "204", description = "Merchant desativado; não há corpo na resposta")
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Identificador do merchant. O id vem da resposta de criação ou da listagem.", example = MERCHANT_ID_EXAMPLE)
            @PathVariable UUID id);
}
