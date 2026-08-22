package br.com.hanrry.reconpay.externalsettlement.controller;

import br.com.hanrry.reconpay.externalsettlement.dto.ExternalSettlementResponseDTO;
import br.com.hanrry.reconpay.externalsettlement.dto.SettlementImportResponseDTO;
import br.com.hanrry.reconpay.externalsettlement.service.ExternalSettlementService;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "External Settlements")
@RequestMapping("/api/merchants/{merchantId}/external-settlements")
public class ExternalSettlementController {

    private final ExternalSettlementService externalSettlementService;

    @GetMapping
    public ResponseEntity<Page<ExternalSettlementResponseDTO>> findAll(
            @PathVariable UUID merchantId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) UUID importId,
            @ParameterObject @PageableDefault(size = 20, sort = "settlementDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(externalSettlementService.findAll(
                merchantId, status, paymentMethod, fromDate, toDate, importId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExternalSettlementResponseDTO> findById(
            @PathVariable UUID merchantId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(externalSettlementService.findById(merchantId, id));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SettlementImportResponseDTO> importCsv(
            @PathVariable UUID merchantId,
            @RequestParam("file") MultipartFile file) {
        SettlementImportResponseDTO importResult = externalSettlementService.importCsv(merchantId, file);
        URI uri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/merchants/{merchantId}/external-settlements/imports/{id}")
                .buildAndExpand(merchantId, importResult.id())
                .toUri();
        return ResponseEntity.created(uri).body(importResult);
    }

    @GetMapping("/imports")
    public ResponseEntity<Page<SettlementImportResponseDTO>> findAllImports(
            @PathVariable UUID merchantId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(externalSettlementService.findAllImports(merchantId, pageable));
    }

    @GetMapping("/imports/{importId}")
    public ResponseEntity<SettlementImportResponseDTO> findImportById(
            @PathVariable UUID merchantId,
            @PathVariable UUID importId) {
        return ResponseEntity.ok(externalSettlementService.findImportById(merchantId, importId));
    }
}
