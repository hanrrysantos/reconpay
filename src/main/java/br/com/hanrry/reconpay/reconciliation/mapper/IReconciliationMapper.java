package br.com.hanrry.reconpay.reconciliation.mapper;

import br.com.hanrry.reconpay.reconciliation.dto.DiscrepancyResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.ReconciliationItemResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.ReconciliationRunResponseDTO;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IReconciliationMapper {

    @Mapping(source = "merchant.id", target = "merchantId")
    ReconciliationRunResponseDTO toRunDTO(ReconciliationRunEntity entity);

    /*
     * Every value below comes from the item's own snapshot columns rather than
     * the linked transaction and settlement rows, so a past run keeps reporting
     * what was actually compared even after those rows change.
     */
    @Mapping(source = "reconciliationRun.id", target = "reconciliationRunId")
    @Mapping(source = "internalTransaction.id", target = "internalTransactionId")
    @Mapping(source = "externalSettlement.id", target = "externalSettlementId")
    @Mapping(target = "paymentMethod", expression = "java(resolvePaymentMethod(entity))")
    @Mapping(target = "installments", expression = "java(resolveInstallments(entity))")
    ReconciliationItemResponseDTO toItemDTO(ReconciliationItemEntity entity);

    DiscrepancyResponseDTO toDiscrepancyDTO(ReconciliationDiscrepancyEntity entity);

    default PaymentMethod resolvePaymentMethod(ReconciliationItemEntity entity) {
        return entity.getTransactionPaymentMethod() != null
                ? entity.getTransactionPaymentMethod()
                : entity.getSettlementPaymentMethod();
    }

    default Integer resolveInstallments(ReconciliationItemEntity entity) {
        return entity.getTransactionInstallments() != null
                ? entity.getTransactionInstallments()
                : entity.getSettlementInstallments();
    }
}
