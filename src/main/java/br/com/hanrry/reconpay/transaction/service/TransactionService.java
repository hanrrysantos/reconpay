package br.com.hanrry.reconpay.transaction.service;

import br.com.hanrry.reconpay.exception.DuplicateExternalReferenceException;
import br.com.hanrry.reconpay.exception.InvalidInstallmentsForPaymentMethodException;
import br.com.hanrry.reconpay.exception.InvalidTransactionStatusTransitionException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.exception.MissingActiveFeeRuleException;
import br.com.hanrry.reconpay.exception.TransactionNotFoundException;
import br.com.hanrry.reconpay.feeRule.entity.FeeRuleEntity;
import br.com.hanrry.reconpay.feeRule.repository.IFeeRuleRepository;
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
import br.com.hanrry.reconpay.transaction.repository.TransactionSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Set<PaymentMethod> SINGLE_INSTALLMENT_METHODS = EnumSet.of(
            PaymentMethod.PIX,
            PaymentMethod.BOLETO,
            PaymentMethod.DEBIT_CARD
    );

    private static final Set<TransactionStatus> TERMINAL_STATUSES = EnumSet.of(
            TransactionStatus.CANCELLED,
            TransactionStatus.REFUNDED,
            TransactionStatus.CHARGEBACK
    );

    private final ITransactionMapper transactionMapper;
    private final IInternalTransactionRepository transactionRepository;
    private final IMerchantRepository merchantRepository;
    private final IFeeRuleRepository feeRuleRepository;

    @Transactional
    public TransactionResponseDTO create(UUID merchantId, CreateTransactionRequestDTO request) {
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));

        if (transactionRepository.existsByMerchant_IdAndExternalReference(
                merchantId, request.externalReference())) {
            throw new DuplicateExternalReferenceException(
                    "Transação já cadastrada com referência externa: " + request.externalReference());
        }

        validateInstallmentsForPaymentMethod(request.paymentMethod(), request.installments());

        FeeRuleEntity feeRule = feeRuleRepository
                .findByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
                        merchantId, request.paymentMethod(), request.installments())
                .orElseThrow(() -> new MissingActiveFeeRuleException(
                        "Regra de taxa ativa não encontrada para este comerciante, método de pagamento e parcelas"));

        InternalTransactionEntity entity = new InternalTransactionEntity();
        entity.setMerchant(merchant);
        entity.setExternalReference(request.externalReference());
        entity.setAmount(request.amount());
        entity.setExpectedNetAmount(calculateExpectedNetAmount(request.amount(), feeRule));
        entity.setPaymentMethod(request.paymentMethod());
        entity.setInstallments(request.installments());
        entity.setStatus(TransactionStatus.APPROVED);
        entity.setTransactionDate(request.transactionDate());

        InternalTransactionEntity savedTransaction = transactionRepository.save(entity);
        return transactionMapper.toDTO(savedTransaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> findAll(
            UUID merchantId,
            TransactionStatus status,
            PaymentMethod paymentMethod,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {
        ensureMerchantExists(merchantId);

        return transactionRepository.findAll(
                        TransactionSpecifications.withFilters(
                                merchantId, status, paymentMethod, fromDate, toDate),
                        pageable)
                .map(transactionMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public TransactionResponseDTO findById(UUID merchantId, UUID id) {
        InternalTransactionEntity transaction = findTransactionForMerchant(merchantId, id);
        return transactionMapper.toDTO(transaction);
    }

    @Transactional
    public TransactionResponseDTO updateStatus(
            UUID merchantId,
            UUID id,
            UpdateTransactionStatusRequestDTO request) {
        InternalTransactionEntity transaction = findTransactionForMerchant(merchantId, id);
        validateStatusTransition(transaction.getStatus(), request.status());

        transaction.setStatus(request.status());
        InternalTransactionEntity savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toDTO(savedTransaction);
    }

    private InternalTransactionEntity findTransactionForMerchant(UUID merchantId, UUID id) {
        ensureMerchantExists(merchantId);

        return transactionRepository.findByIdAndMerchant_Id(id, merchantId)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transação não encontrada com id: " + id));
    }

    private void ensureMerchantExists(UUID merchantId) {
        merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));
    }

    private void validateInstallmentsForPaymentMethod(PaymentMethod paymentMethod, Integer installments) {
        if (SINGLE_INSTALLMENT_METHODS.contains(paymentMethod) && installments > 1) {
            throw new InvalidInstallmentsForPaymentMethodException(
                    "Método de pagamento " + paymentMethod + " não permite parcelamento");
        }
    }

    private void validateStatusTransition(TransactionStatus currentStatus, TransactionStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (TERMINAL_STATUSES.contains(currentStatus)) {
            throw new InvalidTransactionStatusTransitionException(
                    "Transição de status inválida: " + currentStatus + " -> " + newStatus);
        }

        if (currentStatus != TransactionStatus.APPROVED || !TERMINAL_STATUSES.contains(newStatus)) {
            throw new InvalidTransactionStatusTransitionException(
                    "Transição de status inválida: " + currentStatus + " -> " + newStatus);
        }
    }

    private BigDecimal calculateExpectedNetAmount(BigDecimal amount, FeeRuleEntity feeRule) {
        BigDecimal percentageFee = amount
                .multiply(feeRule.getFeePercentage())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        return amount
                .subtract(percentageFee)
                .subtract(feeRule.getFixedFee())
                .setScale(2, RoundingMode.HALF_UP);
    }
}
