package br.com.hanrry.reconpay.transaction.integration;

import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import br.com.hanrry.reconpay.transaction.entity.TransactionImportEntity;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import br.com.hanrry.reconpay.transaction.repository.IInternalTransactionRepository;
import br.com.hanrry.reconpay.transaction.repository.ITransactionImportRepository;
import br.com.hanrry.reconpay.util.IntegrationTestUtils;
import br.com.hanrry.reconpay.util.TestCnpj;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TransactionImportRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired IMerchantRepository merchants;
    @Autowired ITransactionImportRepository imports;
    @Autowired IInternalTransactionRepository transactions;

    @Test
    @Transactional
    void storesABatchAndLinksItsTransactionsToIt() {
        MerchantEntity merchant = merchant("Batch merchant");
        TransactionImportEntity batch = batch(merchant, "vendas-julho.csv", 1);
        InternalTransactionEntity transaction = transaction(merchant, "PED-1");
        transaction.setImportBatch(batch);
        transactions.saveAndFlush(transaction);

        InternalTransactionEntity reread = transactions.findByIdAndMerchant_Id(transaction.getId(), merchant.getId()).orElseThrow();
        assertThat(reread.getImportBatch().getId()).isEqualTo(batch.getId());
        assertThat(batch.getCreatedAt()).isNotNull();
    }

    @Test
    @Transactional
    void findsBatchesOnlyForTheirMerchant() {
        MerchantEntity merchant = merchant("Batch owner");
        MerchantEntity other = merchant("Other owner");
        TransactionImportEntity mine = batch(merchant, "a.csv", 2);
        TransactionImportEntity theirs = batch(other, "b.csv", 3);

        assertThat(imports.findAllByMerchant_Id(merchant.getId(), PageRequest.of(0, 20)).getContent())
                .extracting(TransactionImportEntity::getId).containsExactly(mine.getId());
        assertThat(imports.findByIdAndMerchant_Id(mine.getId(), merchant.getId())).isPresent();
        assertThat(imports.findByIdAndMerchant_Id(theirs.getId(), merchant.getId())).isEmpty();
    }

    @Test
    void transactionCreatedOneByOneHasNoBatch() throws Exception {
        String token = IntegrationTestUtils.obtainAdminToken(mockMvc);
        String merchantId = JsonPath.read(mockMvc.perform(post("/api/merchants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Single create\", \"document\": \"%s\"}".formatted(TestCnpj.random())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(post("/api/merchants/{merchantId}/fee-rules", merchantId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\": \"PIX\", \"installments\": 1, \"feePercentage\": 1.0000, \"fixedFee\": 0.00}"))
                .andExpect(status().isCreated());

        String transactionId = JsonPath.read(mockMvc.perform(post("/api/merchants/{merchantId}/transactions", merchantId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalReference": "UNIT-1", "amount": 50.00, "paymentMethod": "PIX",
                                 "installments": 1, "transactionDate": "2026-07-10"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        assertThat(transactions.findById(UUID.fromString(transactionId)).orElseThrow().getImportBatch()).isNull();
    }

    private MerchantEntity merchant(String name) {
        MerchantEntity merchant = new MerchantEntity();
        merchant.setName(name);
        merchant.setDocument(TestCnpj.random());
        return merchants.saveAndFlush(merchant);
    }

    private TransactionImportEntity batch(MerchantEntity merchant, String fileName, int totalRows) {
        TransactionImportEntity batch = new TransactionImportEntity();
        batch.setMerchant(merchant);
        batch.setFileName(fileName);
        batch.setTotalRows(totalRows);
        return imports.saveAndFlush(batch);
    }

    private InternalTransactionEntity transaction(MerchantEntity merchant, String reference) {
        InternalTransactionEntity transaction = new InternalTransactionEntity();
        transaction.setMerchant(merchant);
        transaction.setExternalReference(reference);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setExpectedNetAmount(new BigDecimal("97.00"));
        transaction.setPaymentMethod(PaymentMethod.PIX);
        transaction.setInstallments(1);
        transaction.setStatus(TransactionStatus.APPROVED);
        transaction.setTransactionDate(LocalDate.of(2026, 7, 10));
        return transaction;
    }
}
