package br.com.hanrry.reconpay.merchant.controller;

import br.com.hanrry.reconpay.merchant.dto.MerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.dto.MerchantResponseDTO;
import br.com.hanrry.reconpay.merchant.dto.UpdateMerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.service.MerchantService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Merchants")
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping
    public ResponseEntity<MerchantResponseDTO> create(
            @Valid @RequestBody MerchantRequestDTO request) {
        MerchantResponseDTO merchant = merchantService.create(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(merchant.id())
                .toUri();
        return ResponseEntity.created(uri).body(merchant);
    }

    @GetMapping
    public ResponseEntity<Page<MerchantResponseDTO>> findAll(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(merchantService.findAllActive(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MerchantResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(merchantService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MerchantResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMerchantRequestDTO request) {
        return ResponseEntity.ok(merchantService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        merchantService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
