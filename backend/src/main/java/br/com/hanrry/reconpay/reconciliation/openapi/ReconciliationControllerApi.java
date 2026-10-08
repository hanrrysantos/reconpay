package br.com.hanrry.reconpay.reconciliation.openapi;

import br.com.hanrry.reconpay.openapi.ApiConflictResponse;
import br.com.hanrry.reconpay.openapi.ApiForbiddenResponse;
import br.com.hanrry.reconpay.openapi.ApiNotFoundResponse;
import br.com.hanrry.reconpay.openapi.ApiUnauthenticatedResponse;
import br.com.hanrry.reconpay.openapi.ApiValidationErrorResponse;
import br.com.hanrry.reconpay.openapi.OpenApiSecuritySchemes;
import br.com.hanrry.reconpay.openapi.OpenApiTags;
import br.com.hanrry.reconpay.reconciliation.dto.DiscrepancyDetailResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.ReconciliationItemResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.ReconciliationRunResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.RunReconciliationRequestDTO;
import br.com.hanrry.reconpay.reconciliation.dto.UpdateDiscrepancyStatusRequestDTO;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.UUID;

@Tag(name = OpenApiTags.RECONCILIATIONS, description = "Run da conciliação e o tratamento das divergências")
@SecurityRequirement(name = OpenApiSecuritySchemes.BEARER_AUTH)
@RequestMapping("/api/merchants/{merchantId}/reconciliations")
public interface ReconciliationControllerApi {

    String ID_EXAMPLE = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    String ID_DESCRIPTION = "Identificador. O id vem da resposta de criação ou da listagem.";

    String CSV_HEADER = "externalReference,result,discrepancyTypes,internalTransactionId,externalSettlementId,transactionAmount,expectedNetAmount,settlementAmount,settlementNetAmount,paymentMethod,installments,transactionStatus,settlementStatus,transactionDate,settlementDate";

    String RUN_JSON = """
            {
              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "merchantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "fromDate": "2026-07-01",
              "toDate": "2026-07-31",
              "status": "COMPLETED",
              "totalItems": 1,
              "matchedCount": 1,
              "divergentCount": 0,
              "createdAt": "2026-08-10T12:00:00Z"
            }
            """;

    String RUN_PAGE_JSON = """
            {
              "content": [
                {
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "merchantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "fromDate": "2026-07-01",
                  "toDate": "2026-07-31",
                  "status": "COMPLETED",
                  "totalItems": 1,
                  "matchedCount": 1,
                  "divergentCount": 0,
                  "createdAt": "2026-08-10T12:00:00Z"
                }
              ],
              "totalElements": 1,
              "totalPages": 1,
              "size": 20,
              "number": 0
            }
            """;

    String ITEM_PAGE_JSON = """
            {
              "content": [
                {
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "reconciliationRunId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "externalReference": "TX-1001",
                  "result": "MATCHED",
                  "transactionAmount": 100.00,
                  "expectedNetAmount": 97.20,
                  "paymentMethod": "CREDIT_CARD",
                  "installments": 1,
                  "transactionStatus": "APPROVED",
                  "transactionDate": "2026-08-10",
                  "discrepancies": [],
                  "createdAt": "2026-08-10T12:00:00Z"
                }
              ],
              "totalElements": 1,
              "totalPages": 1,
              "size": 20,
              "number": 0
            }
            """;

    String DISCREPANCY_JSON = """
            {
              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "type": "FEE_DIVERGENCE",
              "expectedValue": "97.20",
              "actualValue": "97.50",
              "status": "OPEN",
              "adjustments": [],
              "transitions": []
            }
            """;

    String WINDOW_JSON = """
            {
              "fromDate": "2026-07-01",
              "toDate": "2026-07-31"
            }
            """;

    @Operation(
            summary = "Run da conciliação",
            description = """
                    Agenda a conciliação da janela de datas, inclusiva, sobre a data da transação. \
                    O id do merchant vem da resposta de criação ou da listagem. \
                    Responde 404 quando o merchant não existe."""
    )
    @ApiResponse(
            responseCode = "202",
            description = "Conciliação aceita para o run",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReconciliationRunResponseDTO.class),
                    examples = @ExampleObject(name = "run", value = RUN_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @PostMapping
    ResponseEntity<ReconciliationRunResponseDTO> run(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RunReconciliationRequestDTO.class),
                            examples = @ExampleObject(name = "janela", value = WINDOW_JSON)
                    )
            )
            @Valid @RequestBody RunReconciliationRequestDTO request);

    @Operation(
            summary = "Página de runs",
            description = """
                    Lista paginada das conciliações do merchant. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o merchant não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Página de runs da conciliação",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(name = "runs", value = RUN_PAGE_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping
    ResponseEntity<Page<ReconciliationRunResponseDTO>> findAllRuns(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable);

    @Operation(
            summary = "Consulta do run específico",
            description = """
                    Devolve um run da conciliação. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o merchant ou o run não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Run da conciliação encontrado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ReconciliationRunResponseDTO.class),
                    examples = @ExampleObject(name = "run", value = RUN_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping("/{runId}")
    ResponseEntity<ReconciliationRunResponseDTO> findRunById(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID runId);

    @Operation(
            summary = "Página de itens da conciliação",
            description = """
                    Lista paginada dos itens do run. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o merchant ou o run não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Página de itens da conciliação",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(name = "items", value = ITEM_PAGE_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping("/{runId}/items")
    ResponseEntity<Page<ReconciliationItemResponseDTO>> findItems(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID runId,
            @Parameter(description = "Resultado do item", example = "MATCHED")
            @RequestParam(required = false) ReconciliationResult result,
            @Parameter(description = "Tipo de divergência", example = "FEE_DIVERGENCE")
            @RequestParam(required = false) DiscrepancyType discrepancyType,
            @ParameterObject @PageableDefault(size = 20, sort = "externalReference", direction = Sort.Direction.ASC) Pageable pageable);

    @Operation(
            summary = "Exportação CSV da conciliação",
            description = """
                    Devolve o CSV do run. O exemplo é a linha de cabeçalho do arquivo. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o merchant ou o run não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Arquivo CSV da conciliação",
            content = @Content(
                    mediaType = "text/csv",
                    examples = @ExampleObject(name = "csv", value = CSV_HEADER)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping("/{runId}/export")
    void exportCsv(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID runId,
            @Parameter(hidden = true)
            HttpServletResponse response) throws IOException;

    @Operation(
            summary = "Consulta da divergência específica",
            description = """
                    Devolve a divergência com os ajustes e as transições. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o merchant, o run ou a divergência não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Divergência encontrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DiscrepancyDetailResponseDTO.class),
                    examples = @ExampleObject(name = "discrepancy", value = DISCREPANCY_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping("/{runId}/discrepancies/{discrepancyId}")
    ResponseEntity<DiscrepancyDetailResponseDTO> findDiscrepancy(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID runId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID discrepancyId);

    @Operation(
            summary = "Atualização do status da divergência",
            description = """
                    Muda o status da divergência. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o merchant, o run ou a divergência não existe \
                    e 409 quando a transição conflita com o estado atual."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Status da divergência atualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DiscrepancyDetailResponseDTO.class),
                    examples = @ExampleObject(name = "discrepancy", value = DISCREPANCY_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @ApiConflictResponse
    @PatchMapping("/{runId}/discrepancies/{discrepancyId}")
    ResponseEntity<DiscrepancyDetailResponseDTO> changeDiscrepancyStatus(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID runId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID discrepancyId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateDiscrepancyStatusRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "aceite",
                                    value = """
                                            {
                                              "status": "ACCEPTED",
                                              "note": "Divergência aceita após conferência"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpdateDiscrepancyStatusRequestDTO request);
}
