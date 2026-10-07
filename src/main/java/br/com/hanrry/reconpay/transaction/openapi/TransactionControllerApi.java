package br.com.hanrry.reconpay.transaction.openapi;

import br.com.hanrry.reconpay.exception.standardexceptionerror.StandardError;
import br.com.hanrry.reconpay.openapi.ApiForbiddenResponse;
import br.com.hanrry.reconpay.openapi.ApiNotFoundResponse;
import br.com.hanrry.reconpay.openapi.ApiUnauthenticatedResponse;
import br.com.hanrry.reconpay.openapi.ApiValidationErrorResponse;
import br.com.hanrry.reconpay.openapi.OpenApiSecuritySchemes;
import br.com.hanrry.reconpay.openapi.OpenApiTags;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.dto.CreateTransactionRequestDTO;
import br.com.hanrry.reconpay.transaction.dto.TransactionResponseDTO;
import br.com.hanrry.reconpay.transaction.dto.UpdateTransactionStatusRequestDTO;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = OpenApiTags.TRANSACTIONS, description = "Transações de venda registradas no estabelecimento")
@SecurityRequirement(name = OpenApiSecuritySchemes.BEARER_AUTH)
@RequestMapping("/api/merchants/{merchantId}/transactions")
public interface TransactionControllerApi {

    String ID_EXAMPLE = "3fa85f64-5717-4562-b3fc-2c963f66afa6";

    String ID_DESCRIPTION = "Identificador. O id vem da resposta de criação ou da listagem.";

    String TRANSACTION_JSON = """
            {
              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "merchantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "externalReference": "TX-1001",
              "amount": 100.00,
              "expectedNetAmount": 97.20,
              "paymentMethod": "CREDIT_CARD",
              "installments": 1,
              "status": "APPROVED",
              "transactionDate": "2026-08-10",
              "createdAt": "2026-08-10T12:00:00Z",
              "updatedAt": "2026-08-10T12:00:00Z"
            }
            """;

    String CREATE_JSON = """
            {
              "externalReference": "TX-1001",
              "amount": 100.00,
              "paymentMethod": "CREDIT_CARD",
              "installments": 1,
              "transactionDate": "2026-08-10"
            }
            """;

    @Operation(
            summary = "Página de transações",
            description = """
                    Lista paginada das transações do estabelecimento, com filtro opcional. \
                    O id do estabelecimento vem da resposta de criação ou da listagem. \
                    Responde 404 quando o estabelecimento não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Página de transações",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    examples = @ExampleObject(
                            name = "transactions",
                            value = """
                                    {
                                      "content": [
                                        {
                                          "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                          "merchantId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                          "externalReference": "TX-1001",
                                          "amount": 100.00,
                                          "expectedNetAmount": 97.20,
                                          "paymentMethod": "CREDIT_CARD",
                                          "installments": 1,
                                          "status": "APPROVED",
                                          "transactionDate": "2026-08-10",
                                          "createdAt": "2026-08-10T12:00:00Z",
                                          "updatedAt": "2026-08-10T12:00:00Z"
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
    @GetMapping
    ResponseEntity<Page<TransactionResponseDTO>> findAll(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = "Status da transação", example = "APPROVED")
            @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "Método de pagamento", example = "CREDIT_CARD")
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @Parameter(description = "Data inicial da transação, inclusiva", example = "2026-07-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Data final da transação, inclusiva", example = "2026-07-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @ParameterObject @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable);

    @Operation(
            summary = "Consulta da transação específica",
            description = """
                    Devolve uma transação do estabelecimento. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o estabelecimento ou a transação não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Transação encontrada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TransactionResponseDTO.class),
                    examples = @ExampleObject(name = "transaction", value = TRANSACTION_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @GetMapping("/{id}")
    ResponseEntity<TransactionResponseDTO> findById(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID id);

    @Operation(
            summary = "Criação de transação",
            description = """
                    Registra a transação e calcula o líquido esperado pela taxa ativa. \
                    O id do estabelecimento vem da resposta de criação ou da listagem. \
                    Responde 404 quando o estabelecimento não existe."""
    )
    @ApiResponse(
            responseCode = "201",
            description = "Transação criada",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TransactionResponseDTO.class),
                    examples = @ExampleObject(name = "transaction", value = TRANSACTION_JSON)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "Conflito: referência repetida ou taxa ativa ausente para o método e as parcelas",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = StandardError.class),
                    examples = @ExampleObject(
                            name = "conflict",
                            value = """
                                    {
                                      "timestamp": "2026-08-10T15:30:00Z",
                                      "status": 409,
                                      "error": "CONFLICT",
                                      "message": "Referência repetida ou taxa ativa ausente",
                                      "path": "/api/merchants/3fa85f64-5717-4562-b3fc-2c963f66afa6/transactions"
                                    }
                                    """
                    )
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @PostMapping
    ResponseEntity<TransactionResponseDTO> create(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateTransactionRequestDTO.class),
                            examples = @ExampleObject(name = "nova-transacao", value = CREATE_JSON)
                    )
            )
            @Valid @RequestBody CreateTransactionRequestDTO request);

    @Operation(
            summary = "Troca de status da transação",
            description = """
                    Altera o status da transação. \
                    O id vem da resposta de criação ou da listagem. \
                    Responde 404 quando o estabelecimento ou a transação não existe."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Status da transação atualizado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TransactionResponseDTO.class),
                    examples = @ExampleObject(name = "transaction", value = TRANSACTION_JSON)
            )
    )
    @ApiValidationErrorResponse
    @ApiUnauthenticatedResponse
    @ApiForbiddenResponse
    @ApiNotFoundResponse
    @PatchMapping("/{id}/status")
    ResponseEntity<TransactionResponseDTO> updateStatus(
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID merchantId,
            @Parameter(description = ID_DESCRIPTION, example = ID_EXAMPLE)
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateTransactionStatusRequestDTO.class),
                            examples = @ExampleObject(
                                    name = "status",
                                    value = """
                                            {
                                              "status": "CANCELLED"
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody UpdateTransactionStatusRequestDTO request);
}
