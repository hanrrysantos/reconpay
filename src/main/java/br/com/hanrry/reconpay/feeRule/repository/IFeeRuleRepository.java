package br.com.hanrry.reconpay.feeRule.repository;

import br.com.hanrry.reconpay.feeRule.entity.FeeRuleEntity;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IFeeRuleRepository extends JpaRepository<FeeRuleEntity, UUID> {

    Page<FeeRuleEntity> findAllByMerchant_IdAndActiveTrue(UUID merchantId, Pageable pageable);

    Optional<FeeRuleEntity> findByIdAndActiveTrue(UUID id);

    boolean existsByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
            UUID merchantId,
            PaymentMethod paymentMethod,
            Integer installments
    );

    Optional<FeeRuleEntity> findByMerchant_IdAndPaymentMethodAndInstallmentsAndActiveTrue(
            UUID merchantId,
            PaymentMethod paymentMethod,
            Integer installments
    );
}
