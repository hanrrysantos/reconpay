package br.com.hanrry.reconpay.reconciliation.openapi;

import br.com.hanrry.reconpay.openapi.ApiConflictResponse;
import br.com.hanrry.reconpay.openapi.ApiForbiddenResponse;
import br.com.hanrry.reconpay.openapi.ApiNotFoundResponse;
import br.com.hanrry.reconpay.openapi.ApiUnauthenticatedResponse;
import br.com.hanrry.reconpay.openapi.ApiValidationErrorResponse;
import br.com.hanrry.reconpay.openapi.OpenApiSecuritySchemes;
import br.com.hanrry.reconpay.openapi.OpenApiTags;
import br.com.hanrry.reconpay.reconciliation.dto.PeriodResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.PeriodWindowRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = OpenApiTags.RECONCILIATIONS, description = "Run da conciliação e o tratamento das divergências")
@SecurityRequirement(name = OpenApiSecuritySchemes.BEARER_AUTH)
@RequestMapping("/api/merchants/{merchantId}/periods")
public interface PeriodControllerApi {

    String ID_EXAMPLE = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    String WINDOW_JSON = """
            {
              "fromDate": "2026-07-01",
              "toDate": "2026-07-15"
            }
            """;

    String PERIOD_JSON = """
            {
              "runId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "fromDate": "2026-07-01",
              "toDate": "2026-07-15",
              "totalItems": 2,
              "matchedCount": 1,
              "divergentCount": 1,
              "matchRate": 0.5000,
              "openAmount": 0.00,
              "locked": true,
              "lockedAt": "2026-07-16T10:00:30.400Z",
              "closeDurationSeconds": 29
            }
            """;

    @Operation(
            summary = "Leitura do período",
            description = """
                    Devolve a taxa de match, o valor em aberto e a duração da trava da janela. \
                    Responde 404 quando o merchant está inativo ou não há run concluído vigente, \
                    e 409 quando há conciliação pendente ou em execução."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Período da janela, com taxa, valor em aberto e duração",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PeriodResponseDTO.class),
                    examples = @ExampleObject(name = "periodo", value = PERIOD_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @ApiConflictResponse
    @GetMapping
    ResponseEntity<PeriodResponseDTO> get(
            @Parameter(description = "Identificador do merchant. O id vem da resposta de criação ou da listagem.", example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = "Início da janela, inclusivo", example = "2026-07-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Fim da janela, inclusivo", example = "2026-07-15")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate);

    @Operation(
            summary = "Trava do período",
            description = """
                    Trava a janela quando o run vigente está concluído e não há divergência em aberto. \
                    A duração conta os segundos inteiros desde o término do run. \
                    Uma segunda trava responde conflito e conserva o instante original."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Período travado, com valor em aberto zerado e duração da trava",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PeriodResponseDTO.class),
                    examples = @ExampleObject(name = "periodo", value = PERIOD_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @ApiConflictResponse
    @PostMapping("/lock")
    ResponseEntity<PeriodResponseDTO> lock(
            @Parameter(description = "Identificador do merchant. O id vem da resposta de criação ou da listagem.", example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PeriodWindowRequestDTO.class),
                            examples = @ExampleObject(name = "janela", value = WINDOW_JSON)
                    )
            )
            @Valid @RequestBody PeriodWindowRequestDTO request);

    @Operation(
            summary = "Reabertura do período",
            description = """
                    Reabre a janela travada e devolve o mesmo run, com a duração ausente. \
                    Uma segunda reabertura responde conflito e não grava alteração."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Período reaberto, com a mesma taxa e a duração ausente",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PeriodResponseDTO.class),
                    examples = @ExampleObject(name = "periodo", value = PERIOD_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @ApiConflictResponse
    @PostMapping("/unlock")
    ResponseEntity<PeriodResponseDTO> unlock(
            @Parameter(description = "Identificador do merchant. O id vem da resposta de criação ou da listagem.", example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PeriodWindowRequestDTO.class),
                            examples = @ExampleObject(name = "janela", value = WINDOW_JSON)
                    )
            )
            @Valid @RequestBody PeriodWindowRequestDTO request);
}
