package br.com.hanrry.reconpay.auth.controller;

import br.com.hanrry.reconpay.auth.dto.AccessibleMerchantResponseDTO;
import br.com.hanrry.reconpay.auth.dto.MeResponseDTO;
import br.com.hanrry.reconpay.auth.service.MeService;
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
@Tag(name = "Session")
@RequestMapping("/api/me")
public class MeController {

    private final MeService meService;

    @GetMapping
    public ResponseEntity<MeResponseDTO> me() {
        return ResponseEntity.ok(meService.currentUser());
    }

    @GetMapping("/merchants")
    public ResponseEntity<Page<AccessibleMerchantResponseDTO>> merchants(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(meService.accessibleMerchants(pageable));
    }
}
