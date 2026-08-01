package br.com.hanrry.reconpay.feeRule.service;

import br.com.hanrry.reconpay.exception.FeeRuleAlreadyExistsException;
import br.com.hanrry.reconpay.exception.FeeRuleNotFoundException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.feeRule.dto.FeeRuleRequestDTO;
import br.com.hanrry.reconpay.feeRule.dto.FeeRuleResponseDTO;
import br.com.hanrry.reconpay.feeRule.dto.UpdateFeeRuleRequestDTO;
import br.com.hanrry.reconpay.feeRule.entity.FeeRuleEntity;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.feeRule.mapper.IFeeRuleMapper;
import br.com.hanrry.reconpay.feeRule.repository.IFeeRuleRepository;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeeRuleService {

    private final IFeeRuleMapper feeRuleMapper;
    private final IFeeRuleRepository feeRuleRepository;
    private final IMerchantRepository merchantRepository;

    public FeeRuleResponseDTO findById(UUID merchantId, UUID id) {
        FeeRuleEntity feeRule = findActiveFeeRuleForMerchant(merchantId, id);
        return feeRuleMapper.toDTO(feeRule);
    }

    public Page<FeeRuleResponseDTO> findAllByMerchantId(UUID merchantId, Pageable pageable) {
        ensureMerchantExists(merchantId);
        return feeRuleRepository.findAllByMerchant_IdAndActiveTrue(merchantId, pageable)
                .map(feeRuleMapper::toDTO);
    }

    @Transactional
    public FeeRuleResponseDTO create(UUID merchantId, FeeRuleRequestDTO request) {
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));

        ensureUniqueFeeRule(merchantId, request.paymentMethod(), request.installments());

        FeeRuleEntity entity = new FeeRuleEntity();
        entity.setMerchant(merchant);
        entity.setPaymentMethod(request.paymentMethod());
        entity.setInstallments(request.installments());
        entity.setFeePercentage(request.feePercentage());
        entity.setFixedFee(request.fixedFee());

        FeeRuleEntity savedFeeRule = feeRuleRepository.save(entity);
        return feeRuleMapper.toDTO(savedFeeRule);
    }

    @Transactional
    public FeeRuleResponseDTO update(UUID merchantId, UUID id, UpdateFeeRuleRequestDTO request) {
        FeeRuleEntity feeRule = findActiveFeeRuleForMerchant(merchantId, id);

        if (request.paymentMethod() != null || request.installments() != null) {
            PaymentMethod paymentMethod = request.paymentMethod() != null
                    ? request.paymentMethod()
                    : feeRule.getPaymentMethod();

            Integer installments = request.installments() != null
                    ? request.installments()
                    : feeRule.getInstallments();

            boolean sameRule = feeRule.getPaymentMethod().equals(paymentMethod)
                    && feeRule.getInstallments().equals(installments);

            if (!sameRule) {
                ensureUniqueFeeRule(merchantId, paymentMethod, installments);
            }

            feeRule.setPaymentMethod(paymentMethod);
            feeRule.setInstallments(installments);
        }

        if (request.feePercentage() != null) {
            feeRule.setFeePercentage(request.feePercentage());
        }

        if (request.fixedFee() != null) {
            feeRule.setFixedFee(request.fixedFee());
        }

        FeeRuleEntity savedFeeRule = feeRuleRepository.save(feeRule);
        return feeRuleMapper.toDTO(savedFeeRule);
    }

    @Transactional
    public void deleteById(UUID merchantId, UUID id) {
        FeeRuleEntity feeRule = findActiveFeeRuleForMerchant(merchantId, id);
        feeRule.setActive(false);
        feeRuleRepository.save(feeRule);
    }

    private FeeRuleEntity findActiveFeeRuleForMerchant(UUID merchantId, UUID id) {
        ensureMerchantExists(merchantId);

        FeeRuleEntity feeRule = feeRuleRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new FeeRuleNotFoundException("Regra de taxa não encontrada com id: " + id));

        if (!feeRule.getMerchant().getId().equals(merchantId)) {
            throw new FeeRuleNotFoundException("Regra de taxa não encontrada com id: " + id);
        }

        return feeRule;
    }

    private void ensureMerchantExists(UUID merchantId) {
        merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Comerciante não encontrado com id: " + merchantId));
    }

    private void ensureUniqueFeeRule(UUID merchantId, PaymentMethod paymentMethod, Integer installments) {
        boolean alreadyExists = feeRuleRepository
                .existsByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
                        merchantId, paymentMethod, installments);

        if (alreadyExists) {
            throw new FeeRuleAlreadyExistsException(
                    "Regra de taxa já existe para este comerciante, método de pagamento e parcelas");
        }
    }
}
