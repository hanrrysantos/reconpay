package br.com.hanrry.reconpay.reconciliation.openapi;

import br.com.hanrry.reconpay.openapi.OpenApiSecuritySchemes;
import br.com.hanrry.reconpay.openapi.OpenApiTags;
import br.com.hanrry.reconpay.reconciliation.dto.PeriodResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.PeriodWindowRequestDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

@Tag(name = OpenApiTags.RECONCILIATIONS, description = "Run da conciliação e o tratamento das divergências")
@SecurityRequirement(name = OpenApiSecuritySchemes.BEARER_AUTH)
@RequestMapping("/api/merchants/{merchantId}/periods")
public interface PeriodControllerApi {

    @GetMapping
    ResponseEntity<PeriodResponseDTO> get(
            @PathVariable UUID merchantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate);

    @PostMapping("/lock")
    ResponseEntity<PeriodResponseDTO> lock(
            @PathVariable UUID merchantId,
            @Valid @RequestBody PeriodWindowRequestDTO request);

    @PostMapping("/unlock")
    ResponseEntity<PeriodResponseDTO> unlock(
            @PathVariable UUID merchantId,
            @Valid @RequestBody PeriodWindowRequestDTO request);
}
