package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
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

        return new String[] {
                item.getExternalReference(),
                item.getResult().name(),
                discrepancyTypes,
                text(item.getInternalTransaction() != null ? item.getInternalTransaction().getId() : null),
                text(item.getExternalSettlement() != null ? item.getExternalSettlement().getId() : null),
                amount(item.getTransactionAmount()),
                amount(item.getExpectedNetAmount()),
                amount(item.getSettlementAmount()),
                amount(item.getSettlementNetAmount()),
                text(item.getTransactionPaymentMethod() != null
                        ? item.getTransactionPaymentMethod()
                        : item.getSettlementPaymentMethod()),
                text(item.getTransactionInstallments() != null
                        ? item.getTransactionInstallments()
                        : item.getSettlementInstallments()),
                text(item.getTransactionStatus()),
                text(item.getSettlementStatus()),
                text(item.getTransactionDate()),
                text(item.getSettlementDate())
        };
    }

    private String amount(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
