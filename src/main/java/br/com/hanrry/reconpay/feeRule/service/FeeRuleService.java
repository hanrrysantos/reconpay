package br.com.hanrry.reconpay.feeRule.service;

import br.com.hanrry.reconpay.exception.FeeRuleAlreadyExistsException;
import br.com.hanrry.reconpay.exception.FeeRuleNotFoundException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.feeRule.dto.FeeRuleRequestDTO;
import br.com.hanrry.reconpay.feeRule.dto.FeeRuleResponseDTO;
import br.com.hanrry.reconpay.feeRule.dto.UpdateFeeRuleRequestDTO;
import br.com.hanrry.reconpay.feeRule.entity.FeeRuleEntity;
import br.com.hanrry.reconpay.feeRule.mapper.IFeeRuleMapper;
import br.com.hanrry.reconpay.feeRule.repository.IFeeRuleRepository;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeeRuleService {

    private final IFeeRuleMapper feeRuleMapper;
    private final IFeeRuleRepository feeRuleRepository;
    private final IMerchantRepository merchantRepository;

    public FeeRuleResponseDTO findFeeRuleById(UUID id) {
        FeeRuleEntity feeRule = feeRuleRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new FeeRuleNotFoundException("Fee rule not found with id: " + id));

        return feeRuleMapper.toDTO(feeRule);
    }

    public List<FeeRuleResponseDTO> findAllFeeRulesByMerchantId(UUID merchantId) {
        merchantRepository.findByIdAndActiveTrue(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException("Merchant not found with id: " + merchantId));

        List<FeeRuleEntity> feeRules = feeRuleRepository.findAllByMerchant_IdAndActiveTrue(merchantId);

        return feeRuleMapper.toDTOList(feeRules);
    }

    @Transactional
    public FeeRuleResponseDTO createFeeRule(FeeRuleRequestDTO request) {
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(request.merchantId())
                .orElseThrow(() -> new MerchantNotFoundException("Merchant not found with id: " + request.merchantId()));

        boolean alreadyExists = feeRuleRepository.existsByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
                request.merchantId(),
                request.paymentMethod(),
                request.installments()
        );

        if (alreadyExists) {
            throw new FeeRuleAlreadyExistsException("Fee rule already exists for this merchant, payment method and installments");
        }

        FeeRuleEntity entity = new FeeRuleEntity();
        entity.setMerchant(merchant);
        entity.setPaymentMethod(request.paymentMethod());
        entity.setInstallments(request.installments());
        entity.setFeePercentage(request.feePercentage());
        entity.setFixedFee(request.fixedFee());

        FeeRuleEntity saved = feeRuleRepository.save(entity);

        return feeRuleMapper.toDTO(saved);
    }

    @Transactional
    public FeeRuleResponseDTO updateFeeRuleById(UUID id, UpdateFeeRuleRequestDTO request) {
        FeeRuleEntity feeRule = feeRuleRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new FeeRuleNotFoundException("Fee rule not found with id: " + id));

        if (request.paymentMethod() != null || request.installments() != null) {
            var paymentMethod = request.paymentMethod() != null
                    ? request.paymentMethod()
                    : feeRule.getPaymentMethod();

            var installments = request.installments() != null
                    ? request.installments()
                    : feeRule.getInstallments();

            boolean alreadyExists = feeRuleRepository.existsByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
                    feeRule.getMerchant().getId(),
                    paymentMethod,
                    installments
            );

            boolean sameRule = feeRule.getPaymentMethod().equals(paymentMethod)
                    && feeRule.getInstallments().equals(installments);

            if (alreadyExists && !sameRule) {
                throw new FeeRuleAlreadyExistsException("Fee rule already exists for this merchant, payment method and installments");
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
    public void deleteFeeRuleById(UUID id) {
        FeeRuleEntity feeRule = feeRuleRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new FeeRuleNotFoundException("Fee rule not found with id: " + id));

        feeRule.setActive(false);

        feeRuleRepository.save(feeRule);
    }
}
