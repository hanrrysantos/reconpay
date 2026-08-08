package br.com.hanrry.reconpay.transaction.service;

import br.com.hanrry.reconpay.exception.DuplicateExternalReferenceException;
import br.com.hanrry.reconpay.exception.InvalidInstallmentsForPaymentMethodException;
import br.com.hanrry.reconpay.exception.InvalidTransactionStatusTransitionException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.MissingActiveFeeRuleException;
import br.com.hanrry.reconpay.exception.TransactionNotFoundException;
import br.com.hanrry.reconpay.feerule.entity.FeeRuleEntity;
import br.com.hanrry.reconpay.feerule.repository.IFeeRuleRepository;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.dto.CreateTransactionRequestDTO;
import br.com.hanrry.reconpay.transaction.dto.TransactionResponseDTO;
import br.com.hanrry.reconpay.transaction.dto.UpdateTransactionStatusRequestDTO;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import br.com.hanrry.reconpay.transaction.mapper.ITransactionMapper;
import br.com.hanrry.reconpay.transaction.repository.IInternalTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private ITransactionMapper transactionMapper;

    @Mock
    private IInternalTransactionRepository transactionRepository;

    @Mock
    private IMerchantRepository merchantRepository;

    @Mock
    private IFeeRuleRepository feeRuleRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void createShouldCalculateExpectedNetAmountAndPersistTransaction() {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        FeeRuleEntity feeRule = buildFeeRule(merchant, PaymentMethod.CREDIT_CARD, 3);

        CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                "TXN-001",
                new BigDecimal("150.00"),
                PaymentMethod.CREDIT_CARD,
                3,
                LocalDate.parse("2026-07-29")
        );

        InternalTransactionEntity savedEntity = buildTransaction(merchant, request.externalReference());
        savedEntity.setExpectedNetAmount(new BigDecimal("145.00"));

        TransactionResponseDTO expectedResponse = buildTransactionResponse(savedEntity);

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(transactionRepository.existsByMerchant_IdAndExternalReference(merchantId, "TXN-001"))
                .thenReturn(false);
        when(feeRuleRepository.findByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
                merchantId, PaymentMethod.CREDIT_CARD, 3)).thenReturn(Optional.of(feeRule));
        when(transactionRepository.save(any(InternalTransactionEntity.class))).thenReturn(savedEntity);
        when(transactionMapper.toDTO(savedEntity)).thenReturn(expectedResponse);

        TransactionResponseDTO response = transactionService.create(merchantId, request);

        ArgumentCaptor<InternalTransactionEntity> entityCaptor =
                ArgumentCaptor.forClass(InternalTransactionEntity.class);
        verify(transactionRepository).save(entityCaptor.capture());

        InternalTransactionEntity captured = entityCaptor.getValue();
        assertThat(captured.getMerchant()).isEqualTo(merchant);
        assertThat(captured.getExternalReference()).isEqualTo("TXN-001");
        assertThat(captured.getAmount()).isEqualByComparingTo("150.00");
        assertThat(captured.getExpectedNetAmount()).isEqualByComparingTo("145.00");
        assertThat(captured.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void createShouldThrowWhenMerchantNotFound() {
        UUID merchantId = UUID.randomUUID();
        CreateTransactionRequestDTO request = buildCreateRequest("TXN-001", PaymentMethod.PIX, 1);

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(merchantId, request))
                .isInstanceOf(MerchantNotFoundException.class)
                .hasMessageContaining(merchantId.toString());

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createShouldThrowWhenExternalReferenceAlreadyExists() {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        CreateTransactionRequestDTO request = buildCreateRequest("TXN-DUP", PaymentMethod.PIX, 1);

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(transactionRepository.existsByMerchant_IdAndExternalReference(merchantId, "TXN-DUP"))
                .thenReturn(true);

        assertThatThrownBy(() -> transactionService.create(merchantId, request))
                .isInstanceOf(DuplicateExternalReferenceException.class)
                .hasMessage("Transação já cadastrada com referência externa: TXN-DUP");

        verify(feeRuleRepository, never()).findByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
                any(), any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createShouldThrowWhenActiveFeeRuleNotFound() {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        CreateTransactionRequestDTO request = buildCreateRequest("TXN-NO-FEE", PaymentMethod.BOLETO, 1);

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(transactionRepository.existsByMerchant_IdAndExternalReference(merchantId, "TXN-NO-FEE"))
                .thenReturn(false);
        when(feeRuleRepository.findByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
                merchantId, PaymentMethod.BOLETO, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(merchantId, request))
                .isInstanceOf(MissingActiveFeeRuleException.class);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createShouldThrowWhenPixHasMultipleInstallments() {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        CreateTransactionRequestDTO request = buildCreateRequest("TXN-PIX-3X", PaymentMethod.PIX, 3);

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(transactionRepository.existsByMerchant_IdAndExternalReference(merchantId, "TXN-PIX-3X"))
                .thenReturn(false);

        assertThatThrownBy(() -> transactionService.create(merchantId, request))
                .isInstanceOf(InvalidInstallmentsForPaymentMethodException.class)
                .hasMessageContaining("PIX");

        verify(feeRuleRepository, never()).findByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
                any(), any(), any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void findByIdShouldReturnMappedTransaction() {
        UUID merchantId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        InternalTransactionEntity transaction = buildTransaction(merchant, "TXN-001");
        transaction.setId(transactionId);

        TransactionResponseDTO expectedResponse = buildTransactionResponse(transaction);

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(transactionRepository.findByIdAndMerchant_Id(transactionId, merchantId))
                .thenReturn(Optional.of(transaction));
        when(transactionMapper.toDTO(transaction)).thenReturn(expectedResponse);

        TransactionResponseDTO response = transactionService.findById(merchantId, transactionId);

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void findByIdShouldThrowWhenTransactionNotFound() {
        UUID merchantId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(buildMerchant(merchantId)));
        when(transactionRepository.findByIdAndMerchant_Id(transactionId, merchantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findById(merchantId, transactionId))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining(transactionId.toString());
    }

    @Test
    void updateStatusShouldTransitionFromApprovedToRefunded() {
        UUID merchantId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        InternalTransactionEntity transaction = buildTransaction(merchant, "TXN-001");
        transaction.setId(transactionId);
        transaction.setStatus(TransactionStatus.APPROVED);

        InternalTransactionEntity savedTransaction = buildTransaction(merchant, "TXN-001");
        savedTransaction.setId(transactionId);
        savedTransaction.setStatus(TransactionStatus.REFUNDED);

        TransactionResponseDTO expectedResponse = buildTransactionResponse(savedTransaction);

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(transactionRepository.findByIdAndMerchant_Id(transactionId, merchantId))
                .thenReturn(Optional.of(transaction));
        when(transactionRepository.save(transaction)).thenReturn(savedTransaction);
        when(transactionMapper.toDTO(savedTransaction)).thenReturn(expectedResponse);

        TransactionResponseDTO response = transactionService.updateStatus(
                merchantId,
                transactionId,
                new UpdateTransactionStatusRequestDTO(TransactionStatus.REFUNDED));

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.REFUNDED);
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void updateStatusShouldRejectTransitionFromTerminalStatus() {
        UUID merchantId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        InternalTransactionEntity transaction = buildTransaction(merchant, "TXN-001");
        transaction.setId(transactionId);
        transaction.setStatus(TransactionStatus.REFUNDED);

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(transactionRepository.findByIdAndMerchant_Id(transactionId, merchantId))
                .thenReturn(Optional.of(transaction));

        assertThatThrownBy(() -> transactionService.updateStatus(
                merchantId,
                transactionId,
                new UpdateTransactionStatusRequestDTO(TransactionStatus.APPROVED)))
                .isInstanceOf(InvalidTransactionStatusTransitionException.class)
                .hasMessage("Transição de status inválida: REFUNDED -> APPROVED");

        verify(transactionRepository, never()).save(any());
    }

    private CreateTransactionRequestDTO buildCreateRequest(
            String externalReference,
            PaymentMethod paymentMethod,
            int installments) {
        return new CreateTransactionRequestDTO(
                externalReference,
                new BigDecimal("100.00"),
                paymentMethod,
                installments,
                LocalDate.parse("2026-07-29")
        );
    }

    private MerchantEntity buildMerchant(UUID merchantId) {
        MerchantEntity merchant = new MerchantEntity();
        merchant.setId(merchantId);
        merchant.setName("Merchant Test");
        merchant.setDocument("12345678901234");
        merchant.setActive(true);
        return merchant;
    }

    private FeeRuleEntity buildFeeRule(MerchantEntity merchant, PaymentMethod paymentMethod, int installments) {
        FeeRuleEntity feeRule = new FeeRuleEntity();
        feeRule.setId(UUID.randomUUID());
        feeRule.setMerchant(merchant);
        feeRule.setPaymentMethod(paymentMethod);
        feeRule.setInstallments(installments);
        feeRule.setFeePercentage(new BigDecimal("3.0000"));
        feeRule.setFixedFee(new BigDecimal("0.50"));
        feeRule.setActive(true);
        return feeRule;
    }

    private InternalTransactionEntity buildTransaction(MerchantEntity merchant, String externalReference) {
        InternalTransactionEntity entity = new InternalTransactionEntity();
        entity.setMerchant(merchant);
        entity.setExternalReference(externalReference);
        entity.setAmount(new BigDecimal("150.00"));
        entity.setExpectedNetAmount(new BigDecimal("145.00"));
        entity.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        entity.setInstallments(3);
        entity.setStatus(TransactionStatus.APPROVED);
        entity.setTransactionDate(LocalDate.parse("2026-07-29"));
        entity.setCreatedAt(Instant.parse("2026-08-01T12:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-08-01T12:00:00Z"));
        return entity;
    }

    private TransactionResponseDTO buildTransactionResponse(InternalTransactionEntity entity) {
        return new TransactionResponseDTO(
                entity.getId(),
                entity.getMerchant().getId(),
                entity.getExternalReference(),
                entity.getAmount(),
                entity.getExpectedNetAmount(),
                entity.getPaymentMethod(),
                entity.getInstallments(),
                entity.getStatus(),
                entity.getTransactionDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
