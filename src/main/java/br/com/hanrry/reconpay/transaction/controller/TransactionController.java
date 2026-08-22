package br.com.hanrry.reconpay.transaction.controller;

import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.dto.CreateTransactionRequestDTO;
import br.com.hanrry.reconpay.transaction.dto.TransactionResponseDTO;
import br.com.hanrry.reconpay.transaction.dto.UpdateTransactionStatusRequestDTO;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import br.com.hanrry.reconpay.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Transactions")
@RequestMapping("/api/merchants/{merchantId}/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<Page<TransactionResponseDTO>> findAll(
            @PathVariable UUID merchantId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @ParameterObject @PageableDefault(size = 20, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transactionService.findAll(
                merchantId, status, paymentMethod, fromDate, toDate, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> findById(
            @PathVariable UUID merchantId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.findById(merchantId, id));
    }

    @PostMapping
    public ResponseEntity<TransactionResponseDTO> create(
            @PathVariable UUID merchantId,
            @Valid @RequestBody CreateTransactionRequestDTO request) {
        TransactionResponseDTO transaction = transactionService.create(merchantId, request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(transaction.id())
                .toUri();
        return ResponseEntity.created(uri).body(transaction);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TransactionResponseDTO> updateStatus(
            @PathVariable UUID merchantId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTransactionStatusRequestDTO request) {
        return ResponseEntity.ok(transactionService.updateStatus(merchantId, id, request));
    }
}
