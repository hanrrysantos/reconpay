package br.com.hanrry.reconpay.transaction.service;

import br.com.hanrry.reconpay.demo.service.DemoQuotaGuard;
import br.com.hanrry.reconpay.exception.DuplicateTransactionReferenceException;
import br.com.hanrry.reconpay.exception.InvalidTransactionImportException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.PeriodConflictException;
import br.com.hanrry.reconpay.exception.TransactionImportNotFoundException;
import br.com.hanrry.reconpay.exception.TransactionImportValidationException;
import br.com.hanrry.reconpay.externalsettlement.dto.ImportRowErrorDTO;
import br.com.hanrry.reconpay.feerule.entity.FeeRuleEntity;
import br.com.hanrry.reconpay.feerule.repository.IFeeRuleRepository;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.reconciliation.service.PeriodGuard;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.dto.TransactionImportResponseDTO;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import br.com.hanrry.reconpay.transaction.entity.TransactionImportEntity;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import br.com.hanrry.reconpay.transaction.mapper.ITransactionImportMapper;
import br.com.hanrry.reconpay.transaction.repository.IInternalTransactionRepository;
import br.com.hanrry.reconpay.transaction.repository.ITransactionImportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionImportServiceTest {

    private static final String HEADER = "externalReference,amount,paymentMethod,installments,status,transactionDate";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-20T12:00:00Z"), ZoneOffset.UTC);
    private static final UUID MERCHANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock IMerchantRepository merchantRepository;
    @Mock IFeeRuleRepository feeRuleRepository;
    @Mock IInternalTransactionRepository transactionRepository;
    @Mock ITransactionImportRepository transactionImportRepository;
    @Mock ITransactionImportMapper transactionImportMapper;
    @Mock PeriodGuard periodGuard;
    @Mock AuditLogger auditLogger;
    @Mock DemoQuotaGuard demoQuotaGuard;

    private TransactionImportService service;
    private MerchantEntity merchant;

    @BeforeEach
    void setUp() {
        service = new TransactionImportService(new TransactionCsvParser(CLOCK), merchantRepository, feeRuleRepository,
                transactionRepository, transactionImportRepository, transactionImportMapper, periodGuard, auditLogger,
                demoQuotaGuard);
        merchant = new MerchantEntity();
        merchant.setId(MERCHANT_ID);
        lenient().when(merchantRepository.findByIdAndActiveTrue(MERCHANT_ID)).thenReturn(Optional.of(merchant));
        lenient().when(feeRuleRepository.findAllByMerchant_IdAndActiveTrue(MERCHANT_ID)).thenReturn(List.of(
                rule(PaymentMethod.PIX, 1, "1.0000", "0.00"),
                rule(PaymentMethod.CREDIT_CARD, 3, "2.9900", "0.30")));
        lenient().when(transactionRepository.findExistingReferences(eq(MERCHANT_ID), anyCollection())).thenReturn(Set.of());
    }

    private static FeeRuleEntity rule(PaymentMethod method, int installments, String percentage, String fixedFee) {
        FeeRuleEntity rule = new FeeRuleEntity();
        rule.setPaymentMethod(method);
        rule.setInstallments(installments);
        rule.setFeePercentage(new BigDecimal(percentage));
        rule.setFixedFee(new BigDecimal(fixedFee));
        return rule;
    }

    private static MockMultipartFile csv(String fileName, String... lines) {
        return new MockMultipartFile("file", fileName, "text/csv", String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }

    private void assertNothingSaved() {
        verify(transactionImportRepository, never()).save(any());
        verify(transactionRepository, never()).saveAll(anyList());
        verifyNoInteractions(auditLogger);
    }

    @Test
    @SuppressWarnings("unchecked")
    void savesTheBatchAndEveryRowWithTheExpectedNetOfItsRule() {
        when(transactionImportRepository.save(any())).thenAnswer(invocation -> {
            TransactionImportEntity batch = invocation.getArgument(0);
            batch.setId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
            return batch;
        });
        TransactionImportResponseDTO dto = new TransactionImportResponseDTO(null, MERCHANT_ID, "vendas.csv", 2, null);
        when(transactionImportMapper.toDTO(any())).thenReturn(dto);

        TransactionImportResponseDTO result = service.importCsv(MERCHANT_ID, csv("vendas.csv", HEADER,
                "PED-1,100.00,CREDIT_CARD,3,APPROVED,2026-07-10",
                " PED-2 ,50.00,PIX,1,CANCELLED,2026-07-11"));

        assertThat(result).isSameAs(dto);
        ArgumentCaptor<TransactionImportEntity> batch = ArgumentCaptor.forClass(TransactionImportEntity.class);
        verify(transactionImportRepository).save(batch.capture());
        assertThat(batch.getValue().getMerchant()).isSameAs(merchant);
        assertThat(batch.getValue().getFileName()).isEqualTo("vendas.csv");
        assertThat(batch.getValue().getTotalRows()).isEqualTo(2);

        ArgumentCaptor<List<InternalTransactionEntity>> saved = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(saved.capture());
        assertThat(saved.getValue())
                .extracting(InternalTransactionEntity::getExternalReference, InternalTransactionEntity::getAmount,
                        InternalTransactionEntity::getExpectedNetAmount, InternalTransactionEntity::getPaymentMethod,
                        InternalTransactionEntity::getInstallments, InternalTransactionEntity::getStatus,
                        InternalTransactionEntity::getTransactionDate)
                .containsExactly(
                        tuple("PED-1", new BigDecimal("100.00"), new BigDecimal("96.71"), PaymentMethod.CREDIT_CARD, 3,
                                TransactionStatus.APPROVED, LocalDate.of(2026, 7, 10)),
                        tuple("PED-2", new BigDecimal("50.00"), new BigDecimal("49.50"), PaymentMethod.PIX, 1,
                                TransactionStatus.CANCELLED, LocalDate.of(2026, 7, 11)));
        assertThat(saved.getValue()).allSatisfy(transaction -> {
            assertThat(transaction.getMerchant()).isSameAs(merchant);
            assertThat(transaction.getImportBatch()).isSameAs(batch.getValue());
        });
        verify(periodGuard).assertDatesOpen(MERCHANT_ID, List.of(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 11)));
        verify(auditLogger).record(eq("TRANSACTIONS_IMPORTED"), eq("transactionImport"),
                eq(batch.getValue().getId()), eq("merchant=" + MERCHANT_ID + " rows=2"));
    }

    @Test
    void loadsTheActiveRulesOnceForTheWholeFile() {
        when(transactionImportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.importCsv(MERCHANT_ID, csv("v.csv", HEADER,
                "A,10.00,PIX,1,APPROVED,2026-07-10", "B,10.00,PIX,1,APPROVED,2026-07-10", "C,10.00,PIX,1,APPROVED,2026-07-10"));

        verify(feeRuleRepository).findAllByMerchant_IdAndActiveTrue(MERCHANT_ID);
    }

    @Test
    void joinsFormatAndFeeRuleErrorsOrderedByRowAndSavesNothing() {
        assertThatThrownBy(() -> service.importCsv(MERCHANT_ID, csv("v.csv", HEADER,
                "A,10.00,BOLETO,1,APPROVED,2026-07-10",
                "B,0,PIX,1,APPROVED,2026-07-10",
                "C,10.00,CREDIT_CARD,6,APPROVED,2026-07-10",
                "D,0.30,CREDIT_CARD,3,APPROVED,2026-07-10",
                "E,10.00,PIX,1,APPROVED,2026-07-10")))
                .isInstanceOfSatisfying(TransactionImportValidationException.class, ex -> {
                    assertThat(ex.getMessage()).isEqualTo("Erro na importação do CSV");
                    assertThat(ex.getRowErrors()).containsExactly(
                            new ImportRowErrorDTO(2, "Regra de taxa ativa não encontrada para BOLETO em 1x"),
                            new ImportRowErrorDTO(3, "amount deve ser maior que zero"),
                            new ImportRowErrorDTO(4, "Regra de taxa ativa não encontrada para CREDIT_CARD em 6x"),
                            new ImportRowErrorDTO(5, "Valor líquido esperado deve ser maior que zero"));
                });

        assertNothingSaved();
        verify(transactionRepository, never()).findExistingReferences(any(), anyCollection());
        verifyNoInteractions(periodGuard);
    }

    @Test
    void aFeeRuleErrorAloneAlsoRejectsTheWholeFile() {
        assertThatThrownBy(() -> service.importCsv(MERCHANT_ID, csv("v.csv", HEADER,
                "A,10.00,PIX,1,APPROVED,2026-07-10",
                "B,10.00,DEBIT_CARD,1,APPROVED,2026-07-10")))
                .isInstanceOfSatisfying(TransactionImportValidationException.class, ex -> assertThat(ex.getRowErrors())
                        .containsExactly(new ImportRowErrorDTO(3, "Regra de taxa ativa não encontrada para DEBIT_CARD em 1x")));

        assertNothingSaved();
    }

    @Test
    void answersEveryReferenceAlreadyStoredInFileOrderAndSavesNothing() {
        when(transactionRepository.findExistingReferences(eq(MERCHANT_ID), anyCollection())).thenReturn(Set.of("C", "A"));

        assertThatThrownBy(() -> service.importCsv(MERCHANT_ID, csv("v.csv", HEADER,
                "A,10.00,PIX,1,APPROVED,2026-07-10", "B,10.00,PIX,1,APPROVED,2026-07-10", "C,10.00,PIX,1,APPROVED,2026-07-10")))
                .isInstanceOfSatisfying(DuplicateTransactionReferenceException.class, ex -> {
                    assertThat(ex.getMessage()).isEqualTo("Referências externas já cadastradas para este comerciante: A, C");
                    assertThat(ex.getConflictingReferences()).containsExactly("A", "C");
                });

        assertNothingSaved();
        verifyNoInteractions(periodGuard);
    }

    @Test
    void stopsOnALockedPeriodAndSavesNothing() {
        doThrow(new PeriodConflictException("Janela travada de 2026-07-01 a 2026-07-15"))
                .when(periodGuard).assertDatesOpen(eq(MERCHANT_ID), anyList());

        assertThatThrownBy(() -> service.importCsv(MERCHANT_ID, csv("v.csv", HEADER, "A,10.00,PIX,1,APPROVED,2026-07-10")))
                .isInstanceOf(PeriodConflictException.class)
                .hasMessage("Janela travada de 2026-07-01 a 2026-07-15");

        assertNothingSaved();
    }

    @Test
    void rejectsAnEmptyFile() {
        assertThatThrownBy(() -> service.importCsv(MERCHANT_ID, new MockMultipartFile("file", "v.csv", "text/csv", new byte[0])))
                .isInstanceOf(InvalidTransactionImportException.class)
                .hasMessage("Arquivo CSV é obrigatório");
        assertNothingSaved();
    }

    @Test
    void rejectsAFileThatIsNotCsvAndAcceptsTheExtensionInAnyCase() {
        assertThatThrownBy(() -> service.importCsv(MERCHANT_ID, csv("vendas.txt", HEADER, "A,10.00,PIX,1,APPROVED,2026-07-10")))
                .isInstanceOf(InvalidTransactionImportException.class)
                .hasMessage("Arquivo deve ser um CSV (.csv)");
        assertThatThrownBy(() -> service.importCsv(MERCHANT_ID, csv("vendascsv", HEADER, "A,10.00,PIX,1,APPROVED,2026-07-10")))
                .isInstanceOf(InvalidTransactionImportException.class)
                .hasMessage("Arquivo deve ser um CSV (.csv)");
        assertNothingSaved();

        when(transactionImportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.importCsv(MERCHANT_ID, csv("VENDAS.CSV", HEADER, "A,10.00,PIX,1,APPROVED,2026-07-10"));
        verify(transactionImportRepository).save(any());
    }

    @Test
    void rejectsAnInactiveOrMissingMerchantBeforeReadingTheFile() {
        UUID unknown = UUID.randomUUID();
        when(merchantRepository.findByIdAndActiveTrue(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.importCsv(unknown, csv("v.csv", HEADER, "A,10.00,PIX,1,APPROVED,2026-07-10")))
                .isInstanceOf(MerchantNotFoundException.class)
                .hasMessage("Comerciante não encontrado com id: " + unknown);
        assertNothingSaved();
    }

    @Test
    void listsTheMerchantBatches() {
        TransactionImportEntity batch = new TransactionImportEntity();
        TransactionImportResponseDTO dto = new TransactionImportResponseDTO(UUID.randomUUID(), MERCHANT_ID, "a.csv", 1, Instant.now());
        PageRequest page = PageRequest.of(0, 20);
        when(transactionImportRepository.findAllByMerchant_Id(MERCHANT_ID, page)).thenReturn(new PageImpl<>(List.of(batch)));
        when(transactionImportMapper.toDTO(batch)).thenReturn(dto);

        assertThat(service.findAllImports(MERCHANT_ID, page).getContent()).containsExactly(dto);
    }

    @Test
    void findsABatchOfTheMerchantOrAnswersNotFound() {
        UUID mine = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        TransactionImportEntity batch = new TransactionImportEntity();
        TransactionImportResponseDTO dto = new TransactionImportResponseDTO(mine, MERCHANT_ID, "a.csv", 1, Instant.now());
        when(transactionImportRepository.findByIdAndMerchant_Id(mine, MERCHANT_ID)).thenReturn(Optional.of(batch));
        when(transactionImportRepository.findByIdAndMerchant_Id(missing, MERCHANT_ID)).thenReturn(Optional.empty());
        when(transactionImportMapper.toDTO(batch)).thenReturn(dto);

        assertThat(service.findImportById(MERCHANT_ID, mine)).isSameAs(dto);
        assertThatThrownBy(() -> service.findImportById(MERCHANT_ID, missing))
                .isInstanceOf(TransactionImportNotFoundException.class)
                .hasMessage("Importação não encontrada com id: " + missing);
    }

    @Test
    void batchQueriesRejectAMissingMerchant() {
        UUID unknown = UUID.randomUUID();
        when(merchantRepository.findByIdAndActiveTrue(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAllImports(unknown, PageRequest.of(0, 20))).isInstanceOf(MerchantNotFoundException.class);
        assertThatThrownBy(() -> service.findImportById(unknown, UUID.randomUUID())).isInstanceOf(MerchantNotFoundException.class);
    }
}
