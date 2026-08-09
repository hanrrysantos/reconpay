package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReconciliationCsvExporter {

    private static final String[] HEADER = {
            "externalReference",
            "result",
            "discrepancyTypes",
            "internalTransactionId",
            "externalSettlementId",
            "transactionAmount",
            "expectedNetAmount",
            "settlementAmount",
            "settlementNetAmount",
            "paymentMethod",
            "installments",
            "transactionStatus",
            "settlementStatus",
            "transactionDate",
            "settlementDate"
    };

    public byte[] export(List<ReconciliationItemEntity> items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             CSVWriter csvWriter = new CSVWriter(writer)) {

            csvWriter.writeNext(HEADER);

            for (ReconciliationItemEntity item : items) {
                csvWriter.writeNext(toRow(item));
            }

            csvWriter.flush();
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Erro ao gerar relatório CSV", ex);
        }
    }

    private String[] toRow(ReconciliationItemEntity item) {
        String discrepancyTypes = item.getDiscrepancies().stream()
                .map(discrepancy -> discrepancy.getType().name())
                .collect(Collectors.joining(";"));

        var transaction = item.getInternalTransaction();
        var settlement = item.getExternalSettlement();

        return new String[] {
                item.getExternalReference(),
                item.getResult().name(),
                discrepancyTypes,
                transaction != null ? transaction.getId().toString() : "",
                settlement != null ? settlement.getId().toString() : "",
                transaction != null ? transaction.getAmount().toPlainString() : "",
                transaction != null ? transaction.getExpectedNetAmount().toPlainString() : "",
                settlement != null ? settlement.getAmount().toPlainString() : "",
                settlement != null ? settlement.getNetAmount().toPlainString() : "",
                resolvePaymentMethod(transaction, settlement),
                resolveInstallments(transaction, settlement),
                transaction != null ? transaction.getStatus().name() : "",
                settlement != null ? settlement.getStatus().name() : "",
                transaction != null ? transaction.getTransactionDate().toString() : "",
                settlement != null ? settlement.getSettlementDate().toString() : ""
        };
    }

    private String resolvePaymentMethod(InternalTransactionEntity transaction, ExternalSettlementEntity settlement) {
        if (transaction != null) {
            return transaction.getPaymentMethod().name();
        }
        if (settlement != null) {
            return settlement.getPaymentMethod().name();
        }
        return "";
    }

    private String resolveInstallments(InternalTransactionEntity transaction, ExternalSettlementEntity settlement) {
        if (transaction != null) {
            return transaction.getInstallments().toString();
        }
        if (settlement != null) {
            return settlement.getInstallments().toString();
        }
        return "";
    }
}
