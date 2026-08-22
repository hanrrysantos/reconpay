package br.com.hanrry.reconpay.feerule.controller;

import br.com.hanrry.reconpay.feerule.dto.FeeRuleRequestDTO;
import br.com.hanrry.reconpay.feerule.dto.FeeRuleResponseDTO;
import br.com.hanrry.reconpay.feerule.dto.UpdateFeeRuleRequestDTO;
import br.com.hanrry.reconpay.feerule.service.FeeRuleService;
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
@Tag(name = "Fee Rules")
@RequestMapping("/api/merchants/{merchantId}/fee-rules")
public class FeeRuleController {

    private final FeeRuleService feeRuleService;

    @GetMapping
    public ResponseEntity<Page<FeeRuleResponseDTO>> findAll(
            @PathVariable UUID merchantId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(feeRuleService.findAllByMerchantId(merchantId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeeRuleResponseDTO> findById(
            @PathVariable UUID merchantId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(feeRuleService.findById(merchantId, id));
    }

    @PostMapping
    public ResponseEntity<FeeRuleResponseDTO> create(
            @PathVariable UUID merchantId,
            @Valid @RequestBody FeeRuleRequestDTO request) {
        FeeRuleResponseDTO feeRule = feeRuleService.create(merchantId, request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(feeRule.id())
                .toUri();
        return ResponseEntity.created(uri).body(feeRule);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeeRuleResponseDTO> update(
            @PathVariable UUID merchantId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFeeRuleRequestDTO request) {
        return ResponseEntity.ok(feeRuleService.update(merchantId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID merchantId,
            @PathVariable UUID id) {
        feeRuleService.deleteById(merchantId, id);
        return ResponseEntity.noContent().build();
    }
}
