package br.com.hanrry.reconpay.externalsettlement.service;

import br.com.hanrry.reconpay.exception.DuplicateExternalSettlementException;
import br.com.hanrry.reconpay.exception.ExternalSettlementNotFoundException;
import br.com.hanrry.reconpay.exception.InvalidSettlementImportException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.SettlementImportNotFoundException;
import br.com.hanrry.reconpay.externalsettlement.dto.ExternalSettlementResponseDTO;
import br.com.hanrry.reconpay.externalsettlement.dto.SettlementImportResponseDTO;
import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.externalsettlement.entity.SettlementImportEntity;
import br.com.hanrry.reconpay.externalsettlement.mapper.IExternalSettlementMapper;
import br.com.hanrry.reconpay.externalsettlement.mapper.ISettlementImportMapper;
import br.com.hanrry.reconpay.externalsettlement.repository.IExternalSettlementRepository;
import br.com.hanrry.reconpay.externalsettlement.repository.ISettlementImportRepository;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import br.com.hanrry.reconpay.observability.AuditLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalSettlementServiceTest {

    @Mock
    private SettlementCsvParser settlementCsvParser;

    @Mock
    private IExternalSettlementMapper externalSettlementMapper;

    @Mock
    private ISettlementImportMapper settlementImportMapper;

    @Mock
    private IExternalSettlementRepository externalSettlementRepository;

    @Mock
    private ISettlementImportRepository settlementImportRepository;

    @Mock
    private IMerchantRepository merchantRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private ExternalSettlementService externalSettlementService;

    @Captor
    private ArgumentCaptor<List<ExternalSettlementEntity>> settlementsCaptor;

    @Test
    void importCsvShouldPersistImportBatchAndSettlements() throws IOException {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        MultipartFile file = csvFile("settlements.csv", """
                externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate
                EXT-001,150.00,145.00,CREDIT_CARD,3,APPROVED,2026-07-30
                """);

        SettlementCsvParser.ParsedSettlementRow parsedRow = new SettlementCsvParser.ParsedSettlementRow(
                "EXT-001",
                new BigDecimal("150.00"),
                new BigDecimal("145.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                TransactionStatus.APPROVED,
                LocalDate.parse("2026-07-30")
        );

        SettlementImportEntity savedImport = buildImport(merchant, "settlements.csv", 1);
        SettlementImportResponseDTO expectedResponse = buildImportResponse(savedImport);

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(settlementCsvParser.parse(any())).thenReturn(List.of(parsedRow));
        when(externalSettlementRepository.findByMerchant_IdAndExternalReferenceIn(merchantId, List.of("EXT-001")))
                .thenReturn(List.of());
        when(settlementImportRepository.save(any(SettlementImportEntity.class))).thenReturn(savedImport);
        when(settlementImportMapper.toDTO(savedImport)).thenReturn(expectedResponse);

        SettlementImportResponseDTO response = externalSettlementService.importCsv(merchantId, file);

        ArgumentCaptor<SettlementImportEntity> importCaptor = ArgumentCaptor.forClass(SettlementImportEntity.class);
        verify(settlementImportRepository).save(importCaptor.capture());

        SettlementImportEntity capturedImport = importCaptor.getValue();
        assertThat(capturedImport.getMerchant()).isEqualTo(merchant);
        assertThat(capturedImport.getFileName()).isEqualTo("settlements.csv");
        assertThat(capturedImport.getTotalRows()).isEqualTo(1);

        verify(externalSettlementRepository).saveAll(settlementsCaptor.capture());

        ExternalSettlementEntity capturedSettlement = settlementsCaptor.getValue().getFirst();
        assertThat(capturedSettlement.getMerchant()).isEqualTo(merchant);
        assertThat(capturedSettlement.getImportBatch()).isEqualTo(savedImport);
        assertThat(capturedSettlement.getExternalReference()).isEqualTo("EXT-001");
        assertThat(capturedSettlement.getNetAmount()).isEqualByComparingTo("145.00");
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void importCsvShouldThrowWhenMerchantNotFound() {
        UUID merchantId = UUID.randomUUID();
        MultipartFile file = csvFile("settlements.csv", "content");

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> externalSettlementService.importCsv(merchantId, file))
                .isInstanceOf(MerchantNotFoundException.class)
                .hasMessageContaining(merchantId.toString());

        verify(settlementCsvParser, never()).parse(any());
        verify(settlementImportRepository, never()).save(any());
    }

    @Test
    void importCsvShouldRejectEmptyFile() {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        MultipartFile emptyFile = new MockMultipartFile(
                "file",
                "settlements.csv",
                "text/csv",
                new byte[0]
        );

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> externalSettlementService.importCsv(merchantId, emptyFile))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage("Arquivo CSV é obrigatório");

        verify(settlementCsvParser, never()).parse(any());
    }

    @Test
    void importCsvShouldRejectNonCsvFile() {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        MultipartFile invalidFile = new MockMultipartFile(
                "file",
                "settlements.txt",
                "text/plain",
                "content".getBytes(StandardCharsets.UTF_8)
        );

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));

        assertThatThrownBy(() -> externalSettlementService.importCsv(merchantId, invalidFile))
                .isInstanceOf(InvalidSettlementImportException.class)
                .hasMessage("Arquivo deve ser um CSV (.csv)");

        verify(settlementCsvParser, never()).parse(any());
    }

    @Test
    void importCsvShouldThrowWhenExternalReferenceAlreadyExists() throws IOException {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        MultipartFile file = csvFile("settlements.csv", "content");

        SettlementCsvParser.ParsedSettlementRow parsedRow = new SettlementCsvParser.ParsedSettlementRow(
                "EXT-DUP",
                new BigDecimal("100.00"),
                new BigDecimal("98.00"),
                PaymentMethod.PIX,
                1,
                TransactionStatus.APPROVED,
                LocalDate.parse("2026-07-30")
        );

        ExternalSettlementEntity existingSettlement = new ExternalSettlementEntity();
        existingSettlement.setExternalReference("EXT-DUP");

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(settlementCsvParser.parse(any())).thenReturn(List.of(parsedRow));
        when(externalSettlementRepository.findByMerchant_IdAndExternalReferenceIn(merchantId, List.of("EXT-DUP")))
                .thenReturn(List.of(existingSettlement));

        assertThatThrownBy(() -> externalSettlementService.importCsv(merchantId, file))
                .isInstanceOf(DuplicateExternalSettlementException.class)
                .hasMessageContaining("EXT-DUP");

        verify(settlementImportRepository, never()).save(any());
        verify(externalSettlementRepository, never()).saveAll(anyList());
    }

    @Test
    void findByIdShouldReturnMappedSettlement() {
        UUID merchantId = UUID.randomUUID();
        UUID settlementId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        ExternalSettlementEntity settlement = buildSettlement(merchant, settlementId, "EXT-001");
        ExternalSettlementResponseDTO expectedResponse = buildSettlementResponse(settlement);

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(externalSettlementRepository.findByIdAndMerchant_Id(settlementId, merchantId))
                .thenReturn(Optional.of(settlement));
        when(externalSettlementMapper.toDTO(settlement)).thenReturn(expectedResponse);

        ExternalSettlementResponseDTO response = externalSettlementService.findById(merchantId, settlementId);

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void findByIdShouldThrowWhenSettlementNotFound() {
        UUID merchantId = UUID.randomUUID();
        UUID settlementId = UUID.randomUUID();

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(buildMerchant(merchantId)));
        when(externalSettlementRepository.findByIdAndMerchant_Id(settlementId, merchantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> externalSettlementService.findById(merchantId, settlementId))
                .isInstanceOf(ExternalSettlementNotFoundException.class)
                .hasMessageContaining(settlementId.toString());
    }

    @Test
    void findImportByIdShouldThrowWhenImportNotFound() {
        UUID merchantId = UUID.randomUUID();
        UUID importId = UUID.randomUUID();

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(buildMerchant(merchantId)));
        when(settlementImportRepository.findByIdAndMerchant_Id(importId, merchantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> externalSettlementService.findImportById(merchantId, importId))
                .isInstanceOf(SettlementImportNotFoundException.class)
                .hasMessageContaining(importId.toString());
    }

    private MerchantEntity buildMerchant(UUID merchantId) {
        MerchantEntity merchant = new MerchantEntity();
        merchant.setId(merchantId);
        merchant.setName("Merchant Test");
        merchant.setDocument("12345678901234");
        merchant.setActive(true);
        return merchant;
    }

    private SettlementImportEntity buildImport(MerchantEntity merchant, String fileName, int totalRows) {
        SettlementImportEntity importBatch = new SettlementImportEntity();
        importBatch.setId(UUID.randomUUID());
        importBatch.setMerchant(merchant);
        importBatch.setFileName(fileName);
        importBatch.setTotalRows(totalRows);
        importBatch.setCreatedAt(Instant.parse("2026-08-01T12:00:00Z"));
        return importBatch;
    }

    private SettlementImportResponseDTO buildImportResponse(SettlementImportEntity importBatch) {
        return new SettlementImportResponseDTO(
                importBatch.getId(),
                importBatch.getMerchant().getId(),
                importBatch.getFileName(),
                importBatch.getTotalRows(),
                importBatch.getCreatedAt()
        );
    }

    private ExternalSettlementEntity buildSettlement(
            MerchantEntity merchant,
            UUID settlementId,
            String externalReference) {
        ExternalSettlementEntity settlement = new ExternalSettlementEntity();
        settlement.setId(settlementId);
        settlement.setMerchant(merchant);
        settlement.setExternalReference(externalReference);
        settlement.setAmount(new BigDecimal("150.00"));
        settlement.setNetAmount(new BigDecimal("145.00"));
        settlement.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        settlement.setInstallments(3);
        settlement.setStatus(TransactionStatus.APPROVED);
        settlement.setSettlementDate(LocalDate.parse("2026-07-30"));
        settlement.setCreatedAt(Instant.parse("2026-08-01T12:00:00Z"));
        settlement.setUpdatedAt(Instant.parse("2026-08-01T12:00:00Z"));
        return settlement;
    }

    private ExternalSettlementResponseDTO buildSettlementResponse(ExternalSettlementEntity settlement) {
        return new ExternalSettlementResponseDTO(
                settlement.getId(),
                settlement.getMerchant().getId(),
                null,
                settlement.getExternalReference(),
                settlement.getAmount(),
                settlement.getNetAmount(),
                settlement.getPaymentMethod(),
                settlement.getInstallments(),
                settlement.getStatus(),
                settlement.getSettlementDate(),
                settlement.getCreatedAt(),
                settlement.getUpdatedAt()
        );
    }

    private MockMultipartFile csvFile(String fileName, String content) {
        return new MockMultipartFile(
                "file",
                fileName,
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
