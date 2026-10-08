package br.com.hanrry.reconpay.reconciliation.controller;

import br.com.hanrry.reconpay.reconciliation.dto.PeriodResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.PeriodWindowRequestDTO;
import br.com.hanrry.reconpay.reconciliation.openapi.PeriodControllerApi;
import br.com.hanrry.reconpay.reconciliation.service.PeriodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PeriodController implements PeriodControllerApi {

    private final PeriodService periodService;

    @Override
    public ResponseEntity<PeriodResponseDTO> get(
            @PathVariable UUID merchantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(periodService.get(merchantId, fromDate, toDate));
    }

    @Override
    public ResponseEntity<PeriodResponseDTO> lock(
            @PathVariable UUID merchantId,
            @Valid @RequestBody PeriodWindowRequestDTO request) {
        return ResponseEntity.ok(periodService.lock(merchantId, request.fromDate(), request.toDate()));
    }

    @Override
    public ResponseEntity<PeriodResponseDTO> unlock(
            @PathVariable UUID merchantId,
            @Valid @RequestBody PeriodWindowRequestDTO request) {
        return ResponseEntity.ok(periodService.unlock(merchantId, request.fromDate(), request.toDate()));
    }
}
