package br.com.hanrry.reconpay.reconciliation.service;

import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import com.opencsv.CSVReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationCsvExporterTest {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"=SUM(A1:A2)|'=SUM(A1:A2)", "+cmd|'+cmd", "-cmd|'-cmd", "@SUM(A1)|'@SUM(A1)", "TXN-123|TXN-123"}, quoteCharacter = '"')
    void neutralizesSpreadsheetFormulasAndPreservesOrdinaryReferences(String reference, String expected) throws Exception {
        var item = new ReconciliationItemEntity();
        item.setExternalReference(reference);
        item.setResult(ReconciliationResult.MATCHED);
        var exporter = new ReconciliationCsvExporter();
        var output = new StringWriter();
        try (var writer = exporter.open(output)) { exporter.write(writer, List.of(item)); }
        try (var reader = new CSVReader(new StringReader(output.toString()))) {
            reader.readNext();
            assertThat(reader.readNext()[0]).isEqualTo(expected);
        }
    }
}
