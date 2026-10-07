package br.com.hanrry.reconpay.auth.controller;

import br.com.hanrry.reconpay.auth.dto.AccessibleMerchantResponseDTO;
import br.com.hanrry.reconpay.auth.dto.MeResponseDTO;
import br.com.hanrry.reconpay.auth.openapi.MeControllerApi;
import br.com.hanrry.reconpay.auth.service.MeService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeController implements MeControllerApi {

    private final MeService meService;

    @Override
    public ResponseEntity<MeResponseDTO> me() {
        return ResponseEntity.ok(meService.currentUser());
    }

    @Override
    public ResponseEntity<Page<AccessibleMerchantResponseDTO>> merchants(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(meService.accessibleMerchants(pageable));
    }
}
