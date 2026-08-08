package br.com.hanrry.reconpay.feerule.service;

import br.com.hanrry.reconpay.exception.FeeRuleAlreadyExistsException;
import br.com.hanrry.reconpay.exception.FeeRuleNotFoundException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.feerule.dto.FeeRuleRequestDTO;
import br.com.hanrry.reconpay.feerule.dto.FeeRuleResponseDTO;
import br.com.hanrry.reconpay.feerule.dto.UpdateFeeRuleRequestDTO;
import br.com.hanrry.reconpay.feerule.entity.FeeRuleEntity;
import br.com.hanrry.reconpay.feerule.mapper.IFeeRuleMapper;
import br.com.hanrry.reconpay.feerule.repository.IFeeRuleRepository;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeRuleServiceTest {

    @Mock
    private IFeeRuleMapper feeRuleMapper;

    @Mock
    private IFeeRuleRepository feeRuleRepository;

    @Mock
    private IMerchantRepository merchantRepository;

    @InjectMocks
    private FeeRuleService feeRuleService;

    @Test
    void createShouldPersistFeeRuleWhenCombinationIsUnique() {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        FeeRuleRequestDTO request = new FeeRuleRequestDTO(
                PaymentMethod.CREDIT_CARD,
                3,
                new BigDecimal("3.0000"),
                new BigDecimal("0.50")
        );

        FeeRuleEntity savedEntity = buildFeeRule(
                UUID.randomUUID(),
                merchant,
                PaymentMethod.CREDIT_CARD,
                3,
                new BigDecimal("3.0000"),
                new BigDecimal("0.50")
        );
        FeeRuleResponseDTO expectedResponse = toResponseDTO(savedEntity);

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(feeRuleRepository.existsByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
                merchantId, PaymentMethod.CREDIT_CARD, 3)).thenReturn(false);
        when(feeRuleRepository.save(any(FeeRuleEntity.class))).thenReturn(savedEntity);
        when(feeRuleMapper.toDTO(savedEntity)).thenReturn(expectedResponse);

        FeeRuleResponseDTO response = feeRuleService.create(merchantId, request);

        ArgumentCaptor<FeeRuleEntity> entityCaptor = ArgumentCaptor.forClass(FeeRuleEntity.class);
        verify(feeRuleRepository).save(entityCaptor.capture());

        FeeRuleEntity captured = entityCaptor.getValue();
        assertThat(captured.getMerchant()).isEqualTo(merchant);
        assertThat(captured.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(captured.getInstallments()).isEqualTo(3);
        assertThat(captured.getFeePercentage()).isEqualByComparingTo("3.0000");
        assertThat(captured.getFixedFee()).isEqualByComparingTo("0.50");
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void createShouldThrowWhenMerchantNotFound() {
        UUID merchantId = UUID.randomUUID();
        FeeRuleRequestDTO request = new FeeRuleRequestDTO(
                PaymentMethod.PIX,
                1,
                new BigDecimal("1.0000"),
                BigDecimal.ZERO
        );

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> feeRuleService.create(merchantId, request))
                .isInstanceOf(MerchantNotFoundException.class);

        verify(feeRuleRepository, never()).save(any());
    }

    @Test
    void createShouldThrowWhenActiveFeeRuleAlreadyExists() {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        FeeRuleRequestDTO request = new FeeRuleRequestDTO(
                PaymentMethod.PIX,
                1,
                new BigDecimal("1.0000"),
                BigDecimal.ZERO
        );

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(feeRuleRepository.existsByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
                merchantId, PaymentMethod.PIX, 1)).thenReturn(true);

        assertThatThrownBy(() -> feeRuleService.create(merchantId, request))
                .isInstanceOf(FeeRuleAlreadyExistsException.class);

        verify(feeRuleRepository, never()).save(any());
    }

    @Test
    void updateShouldChangeFeeValuesWithoutCheckingUniquenessWhenCombinationIsUnchanged() {
        UUID merchantId = UUID.randomUUID();
        UUID feeRuleId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        FeeRuleEntity feeRule = buildFeeRule(
                feeRuleId,
                merchant,
                PaymentMethod.CREDIT_CARD,
                3,
                new BigDecimal("3.0000"),
                new BigDecimal("0.50")
        );

        FeeRuleEntity savedFeeRule = buildFeeRule(
                feeRuleId,
                merchant,
                PaymentMethod.CREDIT_CARD,
                3,
                new BigDecimal("2.5000"),
                new BigDecimal("0.30")
        );
        FeeRuleResponseDTO expectedResponse = toResponseDTO(savedFeeRule);

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(feeRuleRepository.findByIdAndActiveTrue(feeRuleId)).thenReturn(Optional.of(feeRule));
        when(feeRuleRepository.save(feeRule)).thenReturn(savedFeeRule);
        when(feeRuleMapper.toDTO(savedFeeRule)).thenReturn(expectedResponse);

        FeeRuleResponseDTO response = feeRuleService.update(
                merchantId,
                feeRuleId,
                new UpdateFeeRuleRequestDTO(null, null, new BigDecimal("2.5000"), new BigDecimal("0.30")));

        verify(feeRuleRepository, never()).existsByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
                any(), any(), any());
        assertThat(feeRule.getFeePercentage()).isEqualByComparingTo("2.5000");
        assertThat(feeRule.getFixedFee()).isEqualByComparingTo("0.30");
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void updateShouldCheckUniquenessWhenPaymentMethodOrInstallmentsChange() {
        UUID merchantId = UUID.randomUUID();
        UUID feeRuleId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        FeeRuleEntity feeRule = buildFeeRule(
                feeRuleId,
                merchant,
                PaymentMethod.CREDIT_CARD,
                1,
                new BigDecimal("3.0000"),
                BigDecimal.ZERO
        );

        FeeRuleEntity savedFeeRule = buildFeeRule(
                feeRuleId,
                merchant,
                PaymentMethod.CREDIT_CARD,
                3,
                new BigDecimal("3.0000"),
                BigDecimal.ZERO
        );
        FeeRuleResponseDTO expectedResponse = toResponseDTO(savedFeeRule);

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(feeRuleRepository.findByIdAndActiveTrue(feeRuleId)).thenReturn(Optional.of(feeRule));
        when(feeRuleRepository.existsByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
                merchantId, PaymentMethod.CREDIT_CARD, 3)).thenReturn(false);
        when(feeRuleRepository.save(feeRule)).thenReturn(savedFeeRule);
        when(feeRuleMapper.toDTO(savedFeeRule)).thenReturn(expectedResponse);

        feeRuleService.update(
                merchantId,
                feeRuleId,
                new UpdateFeeRuleRequestDTO(null, 3, null, null));

        verify(feeRuleRepository).existsByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
                merchantId, PaymentMethod.CREDIT_CARD, 3);
        assertThat(feeRule.getInstallments()).isEqualTo(3);
    }

    @Test
    void updateShouldThrowWhenNewCombinationAlreadyExists() {
        UUID merchantId = UUID.randomUUID();
        UUID feeRuleId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        FeeRuleEntity feeRule = buildFeeRule(
                feeRuleId,
                merchant,
                PaymentMethod.CREDIT_CARD,
                1,
                new BigDecimal("3.0000"),
                BigDecimal.ZERO
        );

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(feeRuleRepository.findByIdAndActiveTrue(feeRuleId)).thenReturn(Optional.of(feeRule));
        when(feeRuleRepository.existsByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
                merchantId, PaymentMethod.PIX, 1)).thenReturn(true);

        assertThatThrownBy(() -> feeRuleService.update(
                merchantId,
                feeRuleId,
                new UpdateFeeRuleRequestDTO(PaymentMethod.PIX, 1, null, null)))
                .isInstanceOf(FeeRuleAlreadyExistsException.class);

        verify(feeRuleRepository, never()).save(any());
    }

    @Test
    void findByIdShouldThrowWhenFeeRuleBelongsToAnotherMerchant() {
        UUID merchantId = UUID.randomUUID();
        UUID feeRuleId = UUID.randomUUID();
        MerchantEntity anotherMerchant = buildMerchant(UUID.randomUUID());
        FeeRuleEntity feeRule = buildFeeRule(
                feeRuleId,
                anotherMerchant,
                PaymentMethod.PIX,
                1,
                new BigDecimal("1.0000"),
                BigDecimal.ZERO
        );

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(buildMerchant(merchantId)));
        when(feeRuleRepository.findByIdAndActiveTrue(feeRuleId)).thenReturn(Optional.of(feeRule));

        assertThatThrownBy(() -> feeRuleService.findById(merchantId, feeRuleId))
                .isInstanceOf(FeeRuleNotFoundException.class);
    }

    @Test
    void deleteByIdShouldSoftDeleteFeeRule() {
        UUID merchantId = UUID.randomUUID();
        UUID feeRuleId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId);
        FeeRuleEntity feeRule = buildFeeRule(
                feeRuleId,
                merchant,
                PaymentMethod.PIX,
                1,
                new BigDecimal("1.0000"),
                BigDecimal.ZERO
        );

        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(feeRuleRepository.findByIdAndActiveTrue(feeRuleId)).thenReturn(Optional.of(feeRule));

        feeRuleService.deleteById(merchantId, feeRuleId);

        ArgumentCaptor<FeeRuleEntity> entityCaptor = ArgumentCaptor.forClass(FeeRuleEntity.class);
        verify(feeRuleRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().isActive()).isFalse();
    }

    private MerchantEntity buildMerchant(UUID merchantId) {
        MerchantEntity merchant = new MerchantEntity();
        merchant.setId(merchantId);
        merchant.setName("Merchant Test");
        merchant.setDocument("12345678901234");
        merchant.setActive(true);
        return merchant;
    }

    private FeeRuleEntity buildFeeRule(
            UUID id,
            MerchantEntity merchant,
            PaymentMethod paymentMethod,
            int installments,
            BigDecimal feePercentage,
            BigDecimal fixedFee) {
        FeeRuleEntity feeRule = new FeeRuleEntity();
        feeRule.setId(id);
        feeRule.setMerchant(merchant);
        feeRule.setPaymentMethod(paymentMethod);
        feeRule.setInstallments(installments);
        feeRule.setFeePercentage(feePercentage);
        feeRule.setFixedFee(fixedFee);
        feeRule.setActive(true);
        feeRule.setCreatedAt(Instant.parse("2026-08-01T12:00:00Z"));
        feeRule.setUpdatedAt(Instant.parse("2026-08-01T12:00:00Z"));
        return feeRule;
    }

    private FeeRuleResponseDTO toResponseDTO(FeeRuleEntity feeRule) {
        return new FeeRuleResponseDTO(
                feeRule.getId(),
                feeRule.getMerchant().getId(),
                feeRule.getMerchant().getName(),
                feeRule.getPaymentMethod(),
                feeRule.getInstallments(),
                feeRule.getFeePercentage(),
                feeRule.getFixedFee(),
                feeRule.isActive(),
                feeRule.getCreatedAt()
        );
    }
}
