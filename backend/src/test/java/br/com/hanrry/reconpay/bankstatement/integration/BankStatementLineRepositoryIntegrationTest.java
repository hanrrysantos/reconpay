package br.com.hanrry.reconpay.bankstatement.integration;

import br.com.hanrry.reconpay.bankstatement.entity.BankStatementImportEntity;
import br.com.hanrry.reconpay.bankstatement.entity.BankStatementLineEntity;
import br.com.hanrry.reconpay.bankstatement.repository.IBankStatementLineRepository;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class BankStatementLineRepositoryIntegrationTest extends AbstractIntegrationTest {

    private static final LocalDate FROM = LocalDate.parse("2026-07-01");
    private static final LocalDate TO = LocalDate.parse("2026-07-31");

    @Autowired
    private IBankStatementLineRepository bankStatementLineRepository;

    @Autowired
    private IMerchantRepository merchantRepository;

    @Autowired
    private EntityManager entityManager;

    private UUID merchantId;
    private UUID otherMerchantId;

    @BeforeEach
    void persistLines() {
        MerchantEntity merchant = saveMerchant("Statement A");
        MerchantEntity otherMerchant = saveMerchant("Statement B");
        merchantId = merchant.getId();
        otherMerchantId = otherMerchant.getId();

        BankStatementImportEntity importBatch = persistImport(merchant, "statement-a.csv", 3);
        persistLine(merchant, importBatch, "LN-IN", "150.50", FROM);
        persistLine(merchant, importBatch, "LN-END", "80.00", TO);
        persistLine(merchant, importBatch, "LN-OUT", "10.00", LocalDate.parse("2026-08-01"));

        BankStatementImportEntity otherImport = persistImport(otherMerchant, "statement-b.csv", 1);
        persistLine(otherMerchant, otherImport, "LN-OTHER", "40.00", LocalDate.parse("2026-07-15"));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldFindLinesByMerchantAndMovementDateRange() {
        List<BankStatementLineEntity> found = bankStatementLineRepository
                .findByMerchant_IdAndMovementDateBetween(merchantId, FROM, TO);

        assertThat(found)
                .extracting(BankStatementLineEntity::getLineReference)
                .containsExactlyInAnyOrder("LN-IN", "LN-END");

        BankStatementLineEntity inside = found.stream()
                .filter(line -> line.getLineReference().equals("LN-IN"))
                .findFirst()
                .orElseThrow();
        assertThat(inside.getMerchant().getId()).isEqualTo(merchantId);
        assertThat(inside.getAmount()).isEqualByComparingTo("150.50");
        assertThat(inside.getMovementDate()).isEqualTo(FROM);
        assertThat(inside.getExternalReference()).isNull();

        BankStatementLineEntity end = found.stream()
                .filter(line -> line.getLineReference().equals("LN-END"))
                .findFirst()
                .orElseThrow();
        assertThat(end.getMovementDate()).isEqualTo(TO);
        assertThat(end.getAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    void shouldFindStoredLineReferencesForTheMerchant() {
        List<BankStatementLineEntity> stored = bankStatementLineRepository
                .findByMerchant_IdAndLineReferenceIn(
                        merchantId,
                        List.of("LN-IN", "LN-OTHER", "LN-MISSING"));

        assertThat(stored)
                .extracting(BankStatementLineEntity::getLineReference)
                .containsExactly("LN-IN");
        assertThat(stored.getFirst().getMerchant().getId()).isEqualTo(merchantId);

        List<BankStatementLineEntity> otherStored = bankStatementLineRepository
                .findByMerchant_IdAndLineReferenceIn(otherMerchantId, List.of("LN-OTHER", "LN-IN"));

        assertThat(otherStored)
                .extracting(BankStatementLineEntity::getLineReference)
                .containsExactly("LN-OTHER");
    }

    private MerchantEntity saveMerchant(String name) {
        MerchantEntity merchant = new MerchantEntity();
        merchant.setName(name);
        merchant.setDocument(UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        return merchantRepository.saveAndFlush(merchant);
    }

    private BankStatementImportEntity persistImport(MerchantEntity merchant, String fileName, int totalRows) {
        BankStatementImportEntity importBatch = new BankStatementImportEntity();
        importBatch.setMerchant(merchant);
        importBatch.setFileName(fileName);
        importBatch.setTotalRows(totalRows);
        entityManager.persist(importBatch);
        return importBatch;
    }

    private void persistLine(
            MerchantEntity merchant,
            BankStatementImportEntity importBatch,
            String lineReference,
            String amount,
            LocalDate movementDate) {
        BankStatementLineEntity line = new BankStatementLineEntity();
        line.setMerchant(merchant);
        line.setImportBatch(importBatch);
        line.setLineReference(lineReference);
        line.setAmount(new BigDecimal(amount));
        line.setMovementDate(movementDate);
        entityManager.persist(line);
    }
}
