package br.com.hanrry.reconpay.merchant.controller;

import br.com.hanrry.reconpay.merchant.dto.MerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.dto.MerchantResponseDTO;
import br.com.hanrry.reconpay.merchant.dto.UpdateMerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @PostMapping()
    public ResponseEntity<MerchantResponseDTO> createMerchant(
            @Valid
            @RequestBody MerchantRequestDTO request){
        MerchantResponseDTO merchant = merchantService.createMerchant(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(merchant.id()).toUri();

        return ResponseEntity.created(uri).body(merchant);
    }

    @GetMapping()
    public ResponseEntity<List<MerchantResponseDTO>> findAllMerchantsByActiveTrue() {
        List<MerchantResponseDTO> merchant = merchantService.findAllMerchantsByActiveTrue();

        return ResponseEntity.ok().body(merchant);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<MerchantResponseDTO> findMerchantByIdAndActiveTrue(
            @PathVariable UUID id){
        MerchantResponseDTO merchant = merchantService.findMerchantByIdAndActiveTrue(id);

        return ResponseEntity.ok().body(merchant);
    }

    @PutMapping(value = "/{id}")
    public ResponseEntity<MerchantResponseDTO> updateMerchantById(
            @PathVariable UUID id,
            @RequestBody UpdateMerchantRequestDTO request){
        MerchantResponseDTO merchant = merchantService.updateMerchantById(id, request);

        return ResponseEntity.ok().body(merchant);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteMerchantById(@PathVariable UUID id){
        merchantService.deleteMerchantById(id);

        return ResponseEntity.noContent().build();
    }


}
