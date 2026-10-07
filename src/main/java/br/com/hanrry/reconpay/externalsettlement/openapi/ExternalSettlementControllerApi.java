package br.com.hanrry.reconpay.externalsettlement.openapi;

import br.com.hanrry.reconpay.exception.standardexceptionerror.StandardError;
import br.com.hanrry.reconpay.externalsettlement.dto.ExternalSettlementResponseDTO;
import br.com.hanrry.reconpay.externalsettlement.dto.SettlementImportResponseDTO;
import br.com.hanrry.reconpay.externalsettlement.enums.SettlementLayout;
import br.com.hanrry.reconpay.openapi.ApiForbiddenResponse;
import br.com.hanrry.reconpay.openapi.CsvFilePart;
import br.com.hanrry.reconpay.openapi.ApiNotFoundResponse;
import br.com.hanrry.reconpay.openapi.ApiPayloadTooLargeResponse;
import br.com.hanrry.reconpay.openapi.ApiUnauthenticatedResponse;
import br.com.hanrry.reconpay.openapi.ApiValidationErrorResponse;
import br.com.hanrry.reconpay.openapi.OpenApiSecuritySchemes;
import br.com.hanrry.reconpay.openapi.OpenApiTags;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = OpenApiTags.EXTERNAL_SETTLEMENTS, description = "Liquidações recebidas e a importação do arquivo")
@SecurityRequirement(name = OpenApiSecuritySchemes.BEARER_AUTH)
@RequestMapping("/api/merchants/{merchantId}/external-settlements")
public interface ExternalSettlementControllerApi {

    String ID_EXAMPLE = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    String ID_DESCRIPTION = "Identificador. O id vem da resposta de criação ou da listagem.";

    String MINIMAL_CSV = """
            externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
            TX-1001,100.00,97.50,CREDIT_CARD,1,APPROVED,2026-08-10""";

    String SETTLEMENT_JSON = """
            {
              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "merchantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "importId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "externalReference": "TX-1001",
              "amount": 100.00,
              "netAmount": 97.50,
              "paymentMethod": "CREDIT_CARD",
              "installments": 1,
              "status": "APPROVED",
              "settlementDate": "2026-08-10",
              "createdAt": "2026-08-10T12:00:00Z",
              "updatedAt": "2026-08-10T12:00:00Z"
            }
            """;

    String IMPORT_JSON = """
            {
              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "merchantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "fileName": "liquidacao.csv",
              "totalRows": 1,
              "createdAt": "2026-08-10T12:00:00Z"
            }
            """;

    String SETTLEMENT_PAGE_JSON = """
            {
              "content": [
                {
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "merchantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "importId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "externalReference": "TX-1001",
                  "amount": 100.00,
                  "netAmount": 97.50,
                  "paymentMethod": "CREDIT_CARD",
                  "installments": 1,
                  "status": "APPROVED",
                  "settlementDate": "2026-08-10",
                  "createdAt": "2026-08-10T12:00:00Z",
                  "updatedAt": "2026-08-10T12:00:00Z"
                }
              ],
              "totalElements": 1,
              "totalPages": 1,
              "size": 20,
              "number": 0
            }
            """;

    String IMPORT_PAGE_JSON = """
            {
              "content": [
                {
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "merchantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "fileName": "liquidacao.csv",
                  "totalRows": 1,
                  "createdAt": "2026-08-10T12:00:00Z"
                }
              ],
              "totalElements": 1,
              "totalPages": 1,
              "size": 20,
              "number": 0
            }
            """;

    String ROW_ERRORS_JSON = """
            {
              "timestamp": "2026-08-10T15:30:00Z",
              "status": 400,
              "error": "VALIDATION_ERROR",
              "message": "Erro na importação do CSV",
              "path": "/api/merchants/3fa85f64-5717-4562-b3fc-2c963f66afa6/external-settlements/import",
              "details": {
                "rowErrors": [
                  {
                    "row": 2,
                    "message": "Referência externa é obrigatória"
                  }
                ]
              }
            }
            """;

    String CONFLICTING_REFERENCES_JSON = """
            {
              "timestamp": "2026-08-10T15:30:00Z",
              "status": 409,
              "error": "CONFLICT",
              "message": "Referência externa já importada: TX-1001",
              "path": "/api/merchants/3fa85f64-5717-4562-b3fc-2c963f66afa6/external-settlements/import",
              "details": {
                "conflictingReferences": ["TX-1001"]
              }
            }
            """;

    @Operation(
            summary = "Página de liquidações",
            description = """
                    Lista paginada das liquidações do merchant. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o merchant não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Página de liquidações",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(name = "settlements", value = SETTLEMENT_PAGE_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping
    ResponseEntity<Page<ExternalSettlementResponseDTO>> findAll(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = "Status da liquidação", example = "APPROVED")
            @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "Método de pagamento", example = "CREDIT_CARD")
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @Parameter(description = "Data inicial da liquidação, inclusiva", example = "2026-07-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Data final da liquidação, inclusiva", example = "2026-07-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @RequestParam(required = false) UUID importId,
            @ParameterObject @PageableDefault(size = 20, sort = "settlementDate", direction = Sort.Direction.DESC) Pageable pageable);

    @Operation(
            summary = "Consulta da liquidação específica",
            description = """
                    Devolve uma liquidação do merchant. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o merchant ou a liquidação não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Liquidação encontrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ExternalSettlementResponseDTO.class),
                    examples = @ExampleObject(name = "settlement", value = SETTLEMENT_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping("/{id}")
    ResponseEntity<ExternalSettlementResponseDTO> findById(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID id);

    @Operation(
            summary = "Importação do arquivo de liquidação",
            description = """
                    Envia um CSV na parte file. O id do merchant vem da resposta de criação ou da listagem. \
                    O Try it out não pré-carrega arquivo, então salve este CSV mínimo e selecione-o. \
                    O layout de exemplo é RECONPAY.
                    """ + MINIMAL_CSV
    )
    @ApiResponse(
            responseCode = "201",
            description = "Arquivo de liquidação importado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SettlementImportResponseDTO.class),
                    examples = @ExampleObject(name = "import", value = IMPORT_JSON)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "CSV inválido: cada linha com erro vem em details.rowErrors",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StandardError.class),
                    examples = @ExampleObject(name = "row-errors", value = ROW_ERRORS_JSON)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "Conflito: referências já existentes vêm em details.conflictingReferences",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StandardError.class),
                    examples = @ExampleObject(name = "conflicting-references", value = CONFLICTING_REFERENCES_JSON)
            )
    )
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @ApiPayloadTooLargeResponse
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Parte file com o CSV da liquidação",
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = CsvFilePart.class)
            )
    )
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<SettlementImportResponseDTO> importCsv(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(hidden = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(
                    name = "layout",
                    description = "Layout do CSV. O exemplo é RECONPAY.",
                    example = "RECONPAY"
            )
            @RequestParam(value = "layout", required = false) SettlementLayout layout);

    @Operation(
            summary = "Página de importações de liquidação",
            description = """
                    Lista paginada das importações do merchant. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o merchant não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Página de importações",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(name = "imports", value = IMPORT_PAGE_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping("/imports")
    ResponseEntity<Page<SettlementImportResponseDTO>> findAllImports(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable);

    @Operation(
            summary = "Consulta da importação específica",
            description = """
                    Devolve uma importação de liquidação. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o merchant ou a importação não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Importação encontrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SettlementImportResponseDTO.class),
                    examples = @ExampleObject(name = "import", value = IMPORT_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping("/imports/{importId}")
    ResponseEntity<SettlementImportResponseDTO> findImportById(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID importId);
}
