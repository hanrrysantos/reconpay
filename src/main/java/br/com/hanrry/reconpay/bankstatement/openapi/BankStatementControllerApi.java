package br.com.hanrry.reconpay.bankstatement.openapi;

import br.com.hanrry.reconpay.bankstatement.dto.BankStatementImportResponseDTO;
import br.com.hanrry.reconpay.bankstatement.dto.BankStatementLineResponseDTO;
import br.com.hanrry.reconpay.exception.standardexceptionerror.StandardError;
import br.com.hanrry.reconpay.openapi.ApiForbiddenResponse;
import br.com.hanrry.reconpay.openapi.ApiNotFoundResponse;
import br.com.hanrry.reconpay.openapi.ApiPayloadTooLargeResponse;
import br.com.hanrry.reconpay.openapi.ApiUnauthenticatedResponse;
import br.com.hanrry.reconpay.openapi.ApiValidationErrorResponse;
import br.com.hanrry.reconpay.openapi.CsvFilePart;
import br.com.hanrry.reconpay.openapi.OpenApiSecuritySchemes;
import br.com.hanrry.reconpay.openapi.OpenApiTags;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Tag(name = OpenApiTags.BANK_STATEMENTS, description = "Extrato bancário e a importação das linhas do arquivo")
@SecurityRequirement(name = OpenApiSecuritySchemes.BEARER_AUTH)
@RequestMapping("/api/merchants/{merchantId}/bank-statements")
public interface BankStatementControllerApi {

    String ID_EXAMPLE = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    String ID_DESCRIPTION = "Identificador. O id vem da resposta de criação ou da listagem.";

    String MINIMAL_CSV = """
            lineReference,externalReference,amount,movementDate
            L-1001,TX-1001,97.50,2026-08-10""";

    String LINE_PAGE_JSON = """
            {
              "content": [
                {
                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "lineReference": "L-1001",
                  "externalReference": "TX-1001",
                  "amount": 97.50,
                  "movementDate": "2026-08-10",
                  "importId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
                }
              ],
              "totalElements": 1,
              "totalPages": 1,
              "size": 20,
              "number": 0
            }
            """;

    String IMPORT_JSON = """
            {
              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "merchantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "fileName": "extrato.csv",
              "totalRows": 1,
              "createdAt": "2026-08-10T12:00:00Z"
            }
            """;

    String ROW_ERRORS_JSON = """
            {
              "timestamp": "2026-08-10T15:30:00Z",
              "status": 400,
              "error": "VALIDATION_ERROR",
              "message": "Erro na importação do CSV",
              "path": "/api/merchants/3fa85f64-5717-4562-b3fc-2c963f66afa6/bank-statements/import",
              "details": {
                "rowErrors": [
                  {
                    "row": 2,
                    "message": "Referência da linha é obrigatória"
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
              "message": "Referência da linha já importada: L-1001",
              "path": "/api/merchants/3fa85f64-5717-4562-b3fc-2c963f66afa6/bank-statements/import",
              "details": {
                "conflictingReferences": ["L-1001"]
              }
            }
            """;

    @Operation(
            summary = "Página de linhas do extrato",
            description = """
                    Lista paginada das linhas do extrato do estabelecimento. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o estabelecimento não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Página de linhas do extrato",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(name = "lines", value = LINE_PAGE_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping
    ResponseEntity<Page<BankStatementLineResponseDTO>> findAll(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @RequestParam(required = false) UUID importId,
            @ParameterObject @PageableDefault(size = 20, sort = "movementDate", direction = Sort.Direction.DESC) Pageable pageable);

    @Operation(
            summary = "Importação do arquivo de extrato",
            description = """
                    Envia um CSV na parte file. O id do estabelecimento vem da resposta de criação ou da listagem. \
                    O Try it out não pré-carrega arquivo, então salve este CSV mínimo e selecione-o.
                    """ + MINIMAL_CSV
    )
    @ApiResponse(
            responseCode = "201",
            description = "Arquivo de extrato importado com êxito",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BankStatementImportResponseDTO.class),
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
            description = "Parte file com o CSV do extrato",
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(implementation = CsvFilePart.class)
            )
    )
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<BankStatementImportResponseDTO> importCsv(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(hidden = true)
            @RequestParam("file") MultipartFile file);
}
