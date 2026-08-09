package br.com.hanrry.reconpay.reconciliation.mapper;

import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.reconciliation.dto.DiscrepancyResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.ReconciliationItemResponseDTO;
import br.com.hanrry.reconpay.reconciliation.dto.ReconciliationRunResponseDTO;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IReconciliationMapper {

    @Mapping(source = "merchant.id", target = "merchantId")
    ReconciliationRunResponseDTO toRunDTO(ReconciliationRunEntity entity);

    @Mapping(source = "reconciliationRun.id", target = "reconciliationRunId")
    @Mapping(source = "internalTransaction.id", target = "internalTransactionId")
    @Mapping(source = "externalSettlement.id", target = "externalSettlementId")
    @Mapping(source = "internalTransaction.amount", target = "transactionAmount")
    @Mapping(source = "internalTransaction.expectedNetAmount", target = "expectedNetAmount")
    @Mapping(source = "externalSettlement.amount", target = "settlementAmount")
    @Mapping(source = "externalSettlement.netAmount", target = "settlementNetAmount")
    @Mapping(target = "paymentMethod", expression = "java(resolvePaymentMethod(entity))")
    @Mapping(target = "installments", expression = "java(resolveInstallments(entity))")
    @Mapping(source = "internalTransaction.status", target = "transactionStatus")
    @Mapping(source = "externalSettlement.status", target = "settlementStatus")
    @Mapping(source = "internalTransaction.transactionDate", target = "transactionDate")
    @Mapping(source = "externalSettlement.settlementDate", target = "settlementDate")
    ReconciliationItemResponseDTO toItemDTO(ReconciliationItemEntity entity);

    DiscrepancyResponseDTO toDiscrepancyDTO(ReconciliationDiscrepancyEntity entity);

    default PaymentMethod resolvePaymentMethod(ReconciliationItemEntity entity) {
        InternalTransactionEntity transaction = entity.getInternalTransaction();
        if (transaction != null) {
            return transaction.getPaymentMethod();
        }

        ExternalSettlementEntity settlement = entity.getExternalSettlement();
        if (settlement != null) {
            return settlement.getPaymentMethod();
        }

        return null;
    }

    default Integer resolveInstallments(ReconciliationItemEntity entity) {
        InternalTransactionEntity transaction = entity.getInternalTransaction();
        if (transaction != null) {
            return transaction.getInstallments();
        }

        ExternalSettlementEntity settlement = entity.getExternalSettlement();
        if (settlement != null) {
            return settlement.getInstallments();
        }

        return null;
    }
}
