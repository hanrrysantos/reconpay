package br.com.hanrry.reconpay.auth.controller;

import br.com.hanrry.reconpay.auth.dto.AccessibleMerchantResponseDTO;
import br.com.hanrry.reconpay.auth.dto.MeResponseDTO;
import br.com.hanrry.reconpay.auth.service.MeService;
import br.com.hanrry.reconpay.openapi.OpenApiTags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = OpenApiTags.SESSION, description = "Contexto da sessão autenticada (usuário e merchants acessíveis)")
@RequestMapping("/api/me")
public class MeController {

    private final MeService meService;

    @Operation(
            summary = "Usuário autenticado",
            description = "Retorna id, nome, e-mail, role e flag active do JWT atual.")
    @ApiResponse(
            responseCode = "200",
            description = "Dados do usuário logado",
            content = @Content(schema = @Schema(implementation = MeResponseDTO.class)))
    @GetMapping
    public ResponseEntity<MeResponseDTO> me() {
        return ResponseEntity.ok(meService.currentUser());
    }

    @Operation(
            summary = "Merchants acessíveis",
            description = """
                    Lista paginada de merchants que o usuário pode operar. \
                    OPERATOR vê grants; ADMIN vê todos os merchants ativos.""")
    @ApiResponse(
            responseCode = "200",
            description = "Página de merchants acessíveis")
    @GetMapping("/merchants")
    public ResponseEntity<Page<AccessibleMerchantResponseDTO>> merchants(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(meService.accessibleMerchants(pageable));
    }
}
