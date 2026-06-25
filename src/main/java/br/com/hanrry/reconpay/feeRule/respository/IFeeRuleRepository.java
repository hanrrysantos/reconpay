package br.com.hanrry.reconpay.feeRule.respository;

import br.com.hanrry.reconpay.feeRule.entity.FeeRuleEntity;
import br.com.hanrry.reconpay.feeRule.enums.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IFeeRuleRepository extends JpaRepository<FeeRuleEntity, UUID> {

    List<FeeRuleEntity> findAllByMerchantIdAndActiveTrue(UUID id);

    Optional<FeeRuleEntity> findByIdAndActiveTrue(UUID id);

    boolean existsByMerchantIdAndPaymentMethodAndInstallmentsAndActiveTrue(
            UUID merchantId,
            PaymentMethod paymentMethod,
            Integer installments
    );




}
