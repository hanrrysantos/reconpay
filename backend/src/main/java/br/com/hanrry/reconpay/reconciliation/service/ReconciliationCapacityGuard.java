package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.bankstatement.repository.IBankStatementLineRepository;
import br.com.hanrry.reconpay.externalsettlement.repository.ExternalSettlementSpecifications;
import br.com.hanrry.reconpay.externalsettlement.repository.IExternalSettlementRepository;
import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import br.com.hanrry.reconpay.transaction.repository.IInternalTransactionRepository;
import br.com.hanrry.reconpay.transaction.repository.TransactionSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Refuses a window holding more records than one run can load. The run reads
 * the same three lists, so counting them first keeps an oversized window from
 * exhausting the heap and taking the whole API down with it.
 */
@Component
@RequiredArgsConstructor
public class ReconciliationCapacityGuard {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final IInternalTransactionRepository transactionRepository;
    private final IExternalSettlementRepository externalSettlementRepository;
    private final IBankStatementLineRepository bankStatementLineRepository;
    private final ReconciliationProperties properties;

    /** The refusal message when the window is over the limit; empty when a run may load it. */
    public Optional<String> rejectionFor(UUID merchantId, LocalDate fromDate, LocalDate toDate) {
        long total = countRecords(merchantId, fromDate, toDate);
        if (total <= properties.maxWindowRecords()) {
            return Optional.empty();
        }
        NumberFormat format = NumberFormat.getIntegerInstance(PT_BR);
        return Optional.of("A janela tem %s registros para conciliar; o limite é %s. Divida o período em janelas menores."
                .formatted(format.format(total), format.format(properties.maxWindowRecords())));
    }

    // Same scopes the run loads: settlements and bank lines read the window extended by the lag.
    private long countRecords(UUID merchantId, LocalDate fromDate, LocalDate toDate) {
        LocalDate extendedTo = toDate.plusDays(properties.settlementLagDays());
        return transactionRepository.count(
                        TransactionSpecifications.withFilters(merchantId, null, null, fromDate, toDate))
                + externalSettlementRepository.count(
                        ExternalSettlementSpecifications.withDateRange(merchantId, fromDate, extendedTo))
                + bankStatementLineRepository.countByMerchant_IdAndMovementDateBetween(merchantId, fromDate, extendedTo);
    }
}
