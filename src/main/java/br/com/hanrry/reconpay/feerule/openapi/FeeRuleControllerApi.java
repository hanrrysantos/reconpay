package br.com.hanrry.reconpay.feerule.openapi;

import br.com.hanrry.reconpay.feerule.dto.FeeRuleRequestDTO;
import br.com.hanrry.reconpay.feerule.dto.FeeRuleResponseDTO;
import br.com.hanrry.reconpay.feerule.dto.UpdateFeeRuleRequestDTO;
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

@Tag(name = OpenApiTags.FEE_RULES, description = "Regras de cobrança e taxa do estabelecimento")
@SecurityRequirement(name = OpenApiSecuritySchemes.BEARER_AUTH)
@RequestMapping("/api/merchants/{merchantId}/fee-rules")
public interface FeeRuleControllerApi {

    String ID_EXAMPLE = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    String ID_DESCRIPTION = "Identificador. O id vem da resposta de criação ou da listagem.";

    String FEE_RULE_JSON = """
            {
              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "merchantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "merchantName": "Loja Centro",
              "paymentMethod": "CREDIT_CARD",
              "installments": 1,
              "feePercentage": 2.5,
              "fixedFee": 0.30,
              "active": true,
              "createdAt": "2026-08-10T12:00:00Z"
            }
            """;

    String FEE_RULE_REQUEST_JSON = """
            {
              "paymentMethod": "CREDIT_CARD",
              "installments": 1,
              "feePercentage": 2.5,
              "fixedFee": 0.30
            }
            """;

    @Operation(
            summary = "Página de regras de taxa",
            description = """
                    Lista paginada das regras de taxa do estabelecimento. \
                    O id do estabelecimento vem da resposta de criação ou da listagem. \
                    Responde 404 quando o estabelecimento não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Página de regras de taxa",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(
                            name = "fee-rules",
                            value = """
                                    {
                                      "content": [
                                        {
                                          "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                          "merchantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                          "merchantName": "Loja Centro",
                                          "paymentMethod": "CREDIT_CARD",
                                          "installments": 1,
                                          "feePercentage": 2.5,
                                          "fixedFee": 0.30,
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
    @ApiNotFoundResponse
    @Parameters({
            @Parameter(name = "sort", in = ParameterIn.QUERY, example = "createdAt,desc")
    })
    @GetMapping
    ResponseEntity<Page<FeeRuleResponseDTO>> findAll(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable);

    @Operation(
            summary = "Consulta da regra específica",
            description = """
                    Devolve uma regra de taxa do estabelecimento. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o estabelecimento ou a regra não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Regra de taxa encontrada com êxito",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FeeRuleResponseDTO.class),
                    examples = @ExampleObject(name = "fee-rule", value = FEE_RULE_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping("/{id}")
    ResponseEntity<FeeRuleResponseDTO> findById(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID id);

    @Operation(
            summary = "Criação de regra de taxa",
            description = """
                    Cria a regra de taxa do estabelecimento. \
                    O id do estabelecimento vem da resposta de criação ou da listagem. \
                    Responde 404 quando o estabelecimento não existe e 409 na regra repetida."""
    )
    @ApiResponse(
            responseCode = "201",
            description = "Regra de taxa criada com êxito",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FeeRuleResponseDTO.class),
                    examples = @ExampleObject(name = "fee-rule", value = FEE_RULE_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @ApiConflictResponse
    @PostMapping
    ResponseEntity<FeeRuleResponseDTO> create(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FeeRuleRequestDTO.class),
                            examples = @ExampleObject(name = "nova-regra", value = FEE_RULE_REQUEST_JSON)
                    )
            )
            @Valid @RequestBody FeeRuleRequestDTO request);

    @Operation(
            summary = "Atualização da regra de taxa",
            description = """
                    Altera a regra de taxa. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o estabelecimento ou a regra não existe e 409 na regra repetida."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Regra de taxa atualizada com êxito",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FeeRuleResponseDTO.class),
                    examples = @ExampleObject(name = "fee-rule", value = FEE_RULE_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @ApiConflictResponse
    @PutMapping("/{id}")
    ResponseEntity<FeeRuleResponseDTO> update(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateFeeRuleRequestDTO.class),
                            examples = @ExampleObject(name = "regra", value = FEE_RULE_REQUEST_JSON)
                    )
            )
            @Valid @RequestBody UpdateFeeRuleRequestDTO request);

    @Operation(
            summary = "Exclusão da regra de taxa",
            description = """
                    Desativa a regra de taxa. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o estabelecimento ou a regra não existe."""
    )
    @ApiResponse(responseCode = "204", description = "Regra de taxa desativada; não há corpo na resposta")
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID id);
}
