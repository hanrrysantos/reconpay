package br.com.hanrry.reconpay.auth.openapi;

import br.com.hanrry.reconpay.auth.dto.AccessibleMerchantResponseDTO;
import br.com.hanrry.reconpay.auth.dto.MeResponseDTO;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = OpenApiTags.SESSION, description = "Contexto da sessão autenticada: usuário e merchants acessíveis")
@SecurityRequirement(name = OpenApiSecuritySchemes.BEARER_AUTH)
@RequestMapping("/api/me")
public interface MeControllerApi {

    @Operation(
            summary = "Usuário autenticado",
            description = "Devolve os dados da sessão autenticada: identificador, nome, e-mail, papel e se a conta está ativa."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Dados do usuário logado",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MeResponseDTO.class),
                    examples = @ExampleObject(
                            name = "sessao",
                            value = """
                                    {
                                      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                      "name": "Admin",
                                      "email": "admin@reconpay.local",
                                      "role": "ADMIN",
                                      "active": true
                                    }
                                    """
                    )
            )
    )
    @ApiUnauthenticatedResponse
    @GetMapping
    ResponseEntity<MeResponseDTO> me();

    @Operation(
            summary = "Merchants acessíveis",
            description = """
                    Lista paginada dos estabelecimentos que o usuário pode operar. \
                    OPERATOR vê grants; ADMIN vê todos os merchants ativos."""
    )
    @ApiResponse(
            responseCode = "200",
            description = "Página de merchants acessíveis",
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
                                          "document": "12345678000199"
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
    @Parameters({
            @Parameter(name = "sort", in = ParameterIn.QUERY, example = "name,asc")
    })
    @GetMapping("/merchants")
    ResponseEntity<Page<AccessibleMerchantResponseDTO>> merchants(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable);
}
