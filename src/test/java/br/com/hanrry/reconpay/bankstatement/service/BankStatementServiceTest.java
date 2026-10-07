package br.com.hanrry.reconpay.bankstatement.service;

import br.com.hanrry.reconpay.bankstatement.dto.BankStatementImportResponseDTO;
import br.com.hanrry.reconpay.bankstatement.dto.BankStatementLineResponseDTO;
import br.com.hanrry.reconpay.bankstatement.entity.BankStatementImportEntity;
import br.com.hanrry.reconpay.bankstatement.entity.BankStatementLineEntity;
import br.com.hanrry.reconpay.bankstatement.mapper.IBankStatementImportMapperImpl;
import br.com.hanrry.reconpay.bankstatement.mapper.IBankStatementLineMapperImpl;
import br.com.hanrry.reconpay.bankstatement.repository.IBankStatementImportRepository;
import br.com.hanrry.reconpay.bankstatement.repository.IBankStatementLineRepository;
import br.com.hanrry.reconpay.exception.DuplicateExternalSettlementException;
import br.com.hanrry.reconpay.exception.InvalidSettlementImportException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.SettlementImportValidationException;
import br.com.hanrry.reconpay.externalsettlement.dto.ImportRowErrorDTO;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankStatementServiceTest {

    @Mock
    private BankStatementCsvParser bankStatementCsvParser;

    @Mock
    private IBankStatementLineRepository bankStatementLineRepository;

    @Mock
    private IBankStatementImportRepository bankStatementImportRepository;

    @Mock
    private IMerchantRepository merchantRepository;

    @Mock
    private AuditLogger auditLogger;

    private BankStatementService bankStatementService;

    @BeforeEach
    void setUp() {
        bankStatementService = new BankStatementService(
                bankStatementCsvParser,
                new IBankStatementLineMapperImpl(),
                new IBankStatementImportMapperImpl(),
                bankStatementLineRepository,
                bankStatementImportRepository,
                merchantRepository,
                auditLogger);
    }

    @Test
    void importCsvShouldPersistBatchAndLinesAndAuditAfterSave() throws IOException {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        MultipartFile file = csvFile("statement.csv", "content");
        BankStatementCsvParser.ParsedBankStatementRow withCode = new BankStatementCsvParser.ParsedBankStatementRow(
                "LN-001",
                "TXN-001",
                new BigDecimal("150.50"),
                LocalDate.parse("2026-07-30"));
        BankStatementCsvParser.ParsedBankStatementRow withoutCode = new BankStatementCsvParser.ParsedBankStatementRow(
                "LN-002",
                null,
                new BigDecimal("80.00"),
                LocalDate.parse("2026-07-29"));
        BankStatementImportEntity savedImport = buildImport(merchant, "statement.csv", 2);

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(bankStatementCsvParser.parse(any(InputStream.class))).thenReturn(List.of(withCode, withoutCode));
        when(bankStatementLineRepository.findByMerchant_IdAndLineReferenceIn(
                merchantId, List.of("LN-001", "LN-002"))).thenReturn(List.of());
        when(bankStatementImportRepository.save(any(BankStatementImportEntity.class))).thenReturn(savedImport);

        BankStatementImportResponseDTO response = bankStatementService.importCsv(merchantId, file);

        ArgumentCaptor<BankStatementImportEntity> importCaptor =
                ArgumentCaptor.forClass(BankStatementImportEntity.class);
        verify(bankStatementImportRepository).save(importCaptor.capture());
        assertThat(importCaptor.getValue().getMerchant()).isEqualTo(merchant);
        assertThat(importCaptor.getValue().getFileName()).isEqualTo("statement.csv");
        assertThat(importCaptor.getValue().getTotalRows()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BankStatementLineEntity>> linesCaptor = ArgumentCaptor.forClass(List.class);
        verify(bankStatementLineRepository).saveAll(linesCaptor.capture());
        List<BankStatementLineEntity> savedLines = linesCaptor.getValue();
        assertThat(savedLines).hasSize(2);

        BankStatementLineEntity first = savedLines.get(0);
        assertThat(first.getMerchant()).isEqualTo(merchant);
        assertThat(first.getImportBatch()).isEqualTo(savedImport);
        assertThat(first.getLineReference()).isEqualTo("LN-001");
        assertThat(first.getExternalReference()).isEqualTo("TXN-001");
        assertThat(first.getAmount()).isEqualByComparingTo("150.50");
        assertThat(first.getMovementDate()).isEqualTo(LocalDate.parse("2026-07-30"));

        BankStatementLineEntity second = savedLines.get(1);
        assertThat(second.getLineReference()).isEqualTo("LN-002");
        assertThat(second.getExternalReference()).isNull();
        assertThat(second.getAmount()).isEqualByComparingTo("80.00");
        assertThat(second.getMovementDate()).isEqualTo(LocalDate.parse("2026-07-29"));

        assertThat(response.id()).isEqualTo(savedImport.getId());
        assertThat(response.fileName()).isEqualTo("statement.csv");
        assertThat(response.totalRows()).isEqualTo(2);
        assertThat(response.merchantId()).isEqualTo(merchantId);

        var inOrder = inOrder(bankStatementImportRepository, bankStatementLineRepository, auditLogger);
        inOrder.verify(bankStatementImportRepository).save(any(BankStatementImportEntity.class));
        inOrder.verify(bankStatementLineRepository).saveAll(anyList());
        inOrder.verify(auditLogger).record(
                "BANK_STATEMENTS_IMPORTED",
                "bankStatementImport",
                savedImport.getId(),
                "merchant=" + merchantId + " rows=2");
    }

    @Test
    void importCsvShouldNotSaveWhenParserReportsRowErrors() throws IOException {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        MultipartFile file = csvFile("statement.csv", "content");
        ImportRowErrorDTO rowError = new ImportRowErrorDTO(2, "Referência da linha é obrigatória");

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(bankStatementCsvParser.parse(any(InputStream.class)))
                .thenThrow(new SettlementImportValidationException(
                        "Erro na importação do CSV",
                        List.of(rowError)));

        assertThatThrownBy(() -> bankStatementService.importCsv(merchantId, file))
                .isInstanceOf(SettlementImportValidationException.class)
                .satisfies(ex -> assertThat(((SettlementImportValidationException) ex).getRowErrors())
                        .containsExactly(rowError));

        verify(bankStatementImportRepository, never()).save(any());
        verify(bankStatementLineRepository, never()).saveAll(anyList());
        verifyNoInteractions(auditLogger);
    }

    @Test
    void importCsvShouldNotSaveWhenHeaderIsInvalid() throws IOException {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        MultipartFile file = csvFile("statement.csv", "content");

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(bankStatementCsvParser.parse(any(InputStream.class)))
                .thenThrow(new InvalidSettlementImportException(
                        "Cabeçalho CSV inválido. Esperado: lineReference,externalReference,amount,movementDate"));

        assertThatThrownBy(() -> bankStatementService.importCsv(merchantId, file))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage(
                        "Cabeçalho CSV inválido. Esperado: lineReference,externalReference,amount,movementDate");

        verify(bankStatementImportRepository, never()).save(any());
        verify(bankStatementLineRepository, never()).saveAll(anyList());
        verifyNoInteractions(auditLogger);
    }

    @Test
    void importCsvShouldNotSaveWhenLineReferenceAlreadyExists() throws IOException {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        MultipartFile file = csvFile("statement.csv", "content");
        BankStatementCsvParser.ParsedBankStatementRow parsedRow = new BankStatementCsvParser.ParsedBankStatementRow(
                "LN-DUP",
                "TXN-001",
                new BigDecimal("10.00"),
                LocalDate.parse("2026-07-30"));
        BankStatementLineEntity existing = new BankStatementLineEntity();
        existing.setLineReference("LN-DUP");

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(bankStatementCsvParser.parse(any(InputStream.class))).thenReturn(List.of(parsedRow));
        when(bankStatementLineRepository.findByMerchant_IdAndLineReferenceIn(merchantId, List.of("LN-DUP")))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> bankStatementService.importCsv(merchantId, file))
                .isInstanceOf(DuplicateExternalSettlementException.class)
                .satisfies(ex -> {
                    DuplicateExternalSettlementException duplicate = (DuplicateExternalSettlementException) ex;
                    assertThat(duplicate.getConflictingReferences()).containsExactly("LN-DUP");
                    assertThat(duplicate.getMessage()).contains("LN-DUP");
                });

        verify(bankStatementImportRepository, never()).save(any());
        verify(bankStatementLineRepository, never()).saveAll(anyList());
        verifyNoInteractions(auditLogger);
    }

    @Test
    void importCsvShouldRejectEmptyFileWithoutSaving() {
        UUID merchantId = UUID.randomUUID();
        when(merchantRepository.findByIdAndActiveTrue(merchantId))
                .thenReturn(Optional.of(buildMerchant(merchantId)));

        MultipartFile emptyFile = new MockMultipartFile("file", "statement.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> bankStatementService.importCsv(merchantId, emptyFile))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage("Arquivo CSV é obrigatório");

        verify(bankStatementCsvParser, never()).parse(any(InputStream.class));
        verify(bankStatementImportRepository, never()).save(any());
        verifyNoInteractions(auditLogger);
    }

    @Test
    void importCsvShouldRejectMissingFileWithoutSaving() {
        UUID merchantId = UUID.randomUUID();
        when(merchantRepository.findByIdAndActiveTrue(merchantId))
                .thenReturn(Optional.of(buildMerchant(merchantId)));

        assertThatThrownBy(() -> bankStatementService.importCsv(merchantId, null))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage("Arquivo CSV é obrigatório");

        verify(bankStatementCsvParser, never()).parse(any(InputStream.class));
        verify(bankStatementImportRepository, never()).save(any());
        verifyNoInteractions(auditLogger);
    }

    @Test
    void importCsvShouldNotSaveWhenFileCannotBeRead() throws IOException {
        UUID merchantId = UUID.randomUUID();
        when(merchantRepository.findByIdAndActiveTrue(merchantId))
                .thenReturn(Optional.of(buildMerchant(merchantId)));
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("statement.csv");
        when(file.getInputStream()).thenThrow(new IOException("unreadable"));

        assertThatThrownBy(() -> bankStatementService.importCsv(merchantId, file))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage("Erro ao ler arquivo CSV");

        verify(bankStatementImportRepository, never()).save(any());
        verify(bankStatementLineRepository, never()).saveAll(anyList());
        verifyNoInteractions(auditLogger);
    }

    @Test
    void importCsvShouldRejectNonCsvFileWithoutSaving() {
        UUID merchantId = UUID.randomUUID();
        when(merchantRepository.findByIdAndActiveTrue(merchantId))
                .thenReturn(Optional.of(buildMerchant(merchantId)));

        MultipartFile invalidFile = new MockMultipartFile(
                "file",
                "statement.txt",
                "text/plain",
                "content".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> bankStatementService.importCsv(merchantId, invalidFile))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage("Arquivo deve ser um CSV (.csv)");

        verify(bankStatementCsvParser, never()).parse(any(InputStream.class));
        verify(bankStatementImportRepository, never()).save(any());
        verifyNoInteractions(auditLogger);
    }

    @Test
    void importCsvShouldNotSaveWhenMerchantDoesNotExist() {
        UUID merchantId = UUID.randomUUID();
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bankStatementService.importCsv(merchantId, csvFile("statement.csv", "content")))
                .isInstanceOf(MerchantNotFoundException.class)
                .hasMessageContaining(merchantId.toString());

        verify(bankStatementCsvParser, never()).parse(any(InputStream.class));
        verify(bankStatementImportRepository, never()).save(any());
        verifyNoInteractions(auditLogger);
    }

    @Test
    void findAllShouldReturnOnlyThatMerchantsLines() {
        UUID merchantId = UUID.randomUUID();
        UUID otherMerchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        MerchantEntity otherMerchant = buildMerchant(otherMerchantId);
        BankStatementImportEntity importBatch = buildImport(merchant, "statement.csv", 1);
        BankStatementImportEntity otherImport = buildImport(otherMerchant, "other.csv", 1);
        BankStatementLineEntity line = buildLine(merchant, importBatch, "LN-001", "TXN-001", "150.50", "2026-07-30");
        BankStatementLineEntity otherLine = buildLine(
                otherMerchant, otherImport, "LN-OTHER", null, "40.00", "2026-07-15");
        Pageable pageable = Pageable.unpaged();

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(merchantRepository.findByIdAndActiveTrue(otherMerchantId)).thenReturn(Optional.of(otherMerchant));
        when(bankStatementLineRepository.findByMerchant_Id(merchantId, pageable))
                .thenReturn(new PageImpl<>(List.of(line)));
        when(bankStatementLineRepository.findByMerchant_Id(otherMerchantId, pageable))
                .thenReturn(new PageImpl<>(List.of(otherLine)));

        Page<BankStatementLineResponseDTO> page = bankStatementService.findAll(merchantId, null, pageable);
        Page<BankStatementLineResponseDTO> otherPage = bankStatementService.findAll(otherMerchantId, null, pageable);

        assertThat(page.getContent()).hasSize(1);
        BankStatementLineResponseDTO dto = page.getContent().getFirst();
        assertThat(dto.id()).isEqualTo(line.getId());
        assertThat(dto.lineReference()).isEqualTo("LN-001");
        assertThat(dto.externalReference()).isEqualTo("TXN-001");
        assertThat(dto.amount()).isEqualByComparingTo("150.50");
        assertThat(dto.movementDate()).isEqualTo(LocalDate.parse("2026-07-30"));
        assertThat(dto.importId()).isEqualTo(importBatch.getId());

        assertThat(otherPage.getContent())
                .extracting(BankStatementLineResponseDTO::lineReference)
                .containsExactly("LN-OTHER");
        assertThat(otherPage.getContent().getFirst().externalReference()).isNull();
        assertThat(page.getContent())
                .extracting(BankStatementLineResponseDTO::lineReference)
                .doesNotContain("LN-OTHER");
        assertThat(otherPage.getContent())
                .extracting(BankStatementLineResponseDTO::lineReference)
                .doesNotContain("LN-001");

        verify(bankStatementLineRepository).findByMerchant_Id(eq(merchantId), eq(pageable));
        verify(bankStatementLineRepository, never())
                .findByMerchant_IdAndImportBatch_Id(any(), any(), any());
    }

    @Test
    void findAllShouldFilterByImportId() {
        UUID merchantId = UUID.randomUUID();
        UUID importId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        BankStatementImportEntity importBatch = buildImport(merchant, "statement.csv", 1);
        importBatch.setId(importId);
        BankStatementLineEntity line = buildLine(merchant, importBatch, "LN-001", "TXN-001", "10.00", "2026-07-30");
        Pageable pageable = Pageable.unpaged();

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(bankStatementLineRepository.findByMerchant_IdAndImportBatch_Id(merchantId, importId, pageable))
                .thenReturn(new PageImpl<>(List.of(line)));

        Page<BankStatementLineResponseDTO> page = bankStatementService.findAll(merchantId, importId, pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().importId()).isEqualTo(importId);
        assertThat(page.getContent().getFirst().lineReference()).isEqualTo("LN-001");
        verify(bankStatementLineRepository, never()).findByMerchant_Id(any(), any());
    }

    private MerchantEntity buildMerchant(UUID merchantId) {
        MerchantEntity merchant = new MerchantEntity();
        merchant.setId(merchantId);
        merchant.setName("Merchant Test");
        merchant.setDocument("12345678901234");
        merchant.setActive(true);
        return merchant;
    }

    private BankStatementImportEntity buildImport(MerchantEntity merchant, String fileName, int totalRows) {
        BankStatementImportEntity importBatch = new BankStatementImportEntity();
        importBatch.setId(UUID.randomUUID());
        importBatch.setMerchant(merchant);
        importBatch.setFileName(fileName);
        importBatch.setTotalRows(totalRows);
        importBatch.setCreatedAt(Instant.parse("2026-08-01T12:00:00Z"));
        return importBatch;
    }

    private BankStatementLineEntity buildLine(
            MerchantEntity merchant,
            BankStatementImportEntity importBatch,
            String lineReference,
            String externalReference,
            String amount,
            String movementDate) {
        BankStatementLineEntity line = new BankStatementLineEntity();
        line.setId(UUID.randomUUID());
        line.setMerchant(merchant);
        line.setImportBatch(importBatch);
        line.setLineReference(lineReference);
        line.setExternalReference(externalReference);
        line.setAmount(new BigDecimal(amount));
        line.setMovementDate(LocalDate.parse(movementDate));
        return line;
    }

    private MockMultipartFile csvFile(String fileName, String content) {
        return new MockMultipartFile(
                "file",
                fileName,
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
