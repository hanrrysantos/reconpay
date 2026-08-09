package br.com.hanrry.reconpay.reconciliation.dto;

import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReconciliationItemResponseDTO(
        UUID id,
        UUID reconciliationRunId,
        String externalReference,
        ReconciliationResult result,
        UUID internalTransactionId,
        UUID externalSettlementId,
        BigDecimal transactionAmount,
        BigDecimal expectedNetAmount,
        BigDecimal settlementAmount,
        BigDecimal settlementNetAmount,
        PaymentMethod paymentMethod,
        Integer installments,
        TransactionStatus transactionStatus,
        TransactionStatus settlementStatus,
        LocalDate transactionDate,
        LocalDate settlementDate,
        List<DiscrepancyResponseDTO> discrepancies,
        Instant createdAt
) {
}
