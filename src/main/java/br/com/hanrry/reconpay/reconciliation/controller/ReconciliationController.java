package br.com.hanrry.reconpay.reconciliation.controller;

import br.com.hanrry.reconpay.reconciliation.dto.ReconciliationItemResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.ReconciliationRunResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.RunReconciliationRequestDTO;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import br.com.hanrry.reconpay.reconciliation.service.ReconciliationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Reconciliations")
@RequestMapping("/api/merchants/{merchantId}/reconciliations")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping
    public ResponseEntity<ReconciliationRunResponseDTO> run(
            @PathVariable UUID merchantId,
            @Valid @RequestBody RunReconciliationRequestDTO request) {
        ReconciliationRunResponseDTO run = reconciliationService.run(merchantId, request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(run.id())
                .toUri();
        return ResponseEntity.created(uri).body(run);
    }

    @GetMapping
    public ResponseEntity<Page<ReconciliationRunResponseDTO>> findAllRuns(
            @PathVariable UUID merchantId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(reconciliationService.findAllRuns(merchantId, pageable));
    }

    @GetMapping("/{runId}")
    public ResponseEntity<ReconciliationRunResponseDTO> findRunById(
            @PathVariable UUID merchantId,
            @PathVariable UUID runId) {
        return ResponseEntity.ok(reconciliationService.findRunById(merchantId, runId));
    }

    @GetMapping("/{runId}/items")
    public ResponseEntity<Page<ReconciliationItemResponseDTO>> findItems(
            @PathVariable UUID merchantId,
            @PathVariable UUID runId,
            @RequestParam(required = false) ReconciliationResult result,
            @RequestParam(required = false) DiscrepancyType discrepancyType,
            @ParameterObject @PageableDefault(size = 20, sort = "externalReference", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(reconciliationService.findItems(
                merchantId, runId, result, discrepancyType, pageable));
    }

    @GetMapping("/{runId}/export")
    public ResponseEntity<byte[]> exportCsv(
            @PathVariable UUID merchantId,
            @PathVariable UUID runId) {
        byte[] csvContent = reconciliationService.exportCsv(merchantId, runId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reconciliation-" + runId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }
}
