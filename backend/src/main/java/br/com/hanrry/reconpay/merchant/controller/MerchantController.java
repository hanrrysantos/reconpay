package br.com.hanrry.reconpay.merchant.controller;

import br.com.hanrry.reconpay.merchant.dto.MerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.dto.MerchantResponseDTO;
import br.com.hanrry.reconpay.merchant.dto.UpdateMerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.openapi.MerchantControllerApi;
import br.com.hanrry.reconpay.merchant.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MerchantController implements MerchantControllerApi {

    private final MerchantService merchantService;

    @Override
    public ResponseEntity<MerchantResponseDTO> create(@Valid @RequestBody MerchantRequestDTO request) {
        MerchantResponseDTO merchant = merchantService.create(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(merchant.id())
                .toUri();
        return ResponseEntity.created(uri).body(merchant);
    }

    @Override
    public ResponseEntity<Page<MerchantResponseDTO>> findAll(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(merchantService.findAllActive(pageable));
    }

    @Override
    public ResponseEntity<MerchantResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(merchantService.findById(id));
    }

    @Override
    public ResponseEntity<MerchantResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMerchantRequestDTO request) {
        return ResponseEntity.ok(merchantService.update(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        merchantService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
