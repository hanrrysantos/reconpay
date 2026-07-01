package br.com.hanrry.reconpay.feeRule.controller;

import br.com.hanrry.reconpay.feeRule.dto.FeeRuleRequestDTO;
import br.com.hanrry.reconpay.feeRule.dto.FeeRuleResponseDTO;
import br.com.hanrry.reconpay.feeRule.dto.UpdateFeeRuleRequestDTO;
import br.com.hanrry.reconpay.feeRule.service.FeeRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/fee-rules")
public class FeeRuleController {

    private final FeeRuleService feeRuleService;

    @GetMapping("/{id}")
    public ResponseEntity<FeeRuleResponseDTO> findFeeRuleById(
            @PathVariable UUID id) {
        FeeRuleResponseDTO feeRule = feeRuleService.findFeeRuleById(id);
        return ResponseEntity.ok().body(feeRule);
    }

    @GetMapping("/by-merchant/{merchantId}")
    public ResponseEntity<List<FeeRuleResponseDTO>> findAllFeeRulesByMerchantId(
            @PathVariable UUID merchantId) {
        List<FeeRuleResponseDTO> feeRules = feeRuleService.findAllFeeRulesByMerchantId(merchantId);
        return ResponseEntity.ok().body(feeRules);
    }

    @PostMapping
    public ResponseEntity<FeeRuleResponseDTO> createFeeRule(
            @Valid
            @RequestBody FeeRuleRequestDTO request) {
        FeeRuleResponseDTO feeRule = feeRuleService.createFeeRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(feeRule);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeeRuleResponseDTO> updateFeeRuleById(
            @Valid
            @RequestBody UpdateFeeRuleRequestDTO request,
            @PathVariable UUID id) {
        FeeRuleResponseDTO feeRule = feeRuleService.updateFeeRuleById(id, request);
        return ResponseEntity.ok().body(feeRule);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeeRuleById(@PathVariable UUID id){
        feeRuleService.deleteFeeRuleById(id);
        return ResponseEntity.noContent().build();
    }

}
