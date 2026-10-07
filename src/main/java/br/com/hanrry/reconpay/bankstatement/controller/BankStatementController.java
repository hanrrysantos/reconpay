package br.com.hanrry.reconpay.bankstatement.controller;

import br.com.hanrry.reconpay.bankstatement.dto.BankStatementImportResponseDTO;
import br.com.hanrry.reconpay.bankstatement.dto.BankStatementLineResponseDTO;
import br.com.hanrry.reconpay.bankstatement.service.BankStatementService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Bank Statements")
@RequestMapping("/api/merchants/{merchantId}/bank-statements")
public class BankStatementController {

    private final BankStatementService bankStatementService;

    @GetMapping
    public ResponseEntity<Page<BankStatementLineResponseDTO>> findAll(
            @PathVariable UUID merchantId,
            @RequestParam(required = false) UUID importId,
            @ParameterObject @PageableDefault(size = 20, sort = "movementDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(bankStatementService.findAll(merchantId, importId, pageable));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BankStatementImportResponseDTO> importCsv(
            @PathVariable UUID merchantId,
            @RequestParam("file") MultipartFile file) {
        BankStatementImportResponseDTO importResult = bankStatementService.importCsv(merchantId, file);
        URI uri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/merchants/{merchantId}/bank-statements")
                .buildAndExpand(merchantId)
                .toUri();
        return ResponseEntity.created(uri).body(importResult);
    }
}
