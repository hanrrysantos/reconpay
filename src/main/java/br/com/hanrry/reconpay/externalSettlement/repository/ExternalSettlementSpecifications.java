package br.com.hanrry.reconpay.externalSettlement.repository;

import br.com.hanrry.reconpay.externalSettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class ExternalSettlementSpecifications {

    private ExternalSettlementSpecifications() {
    }

    public static Specification<ExternalSettlementEntity> withFilters(
            UUID merchantId,
            TransactionStatus status,
            PaymentMethod paymentMethod,
            LocalDate fromDate,
            LocalDate toDate,
            UUID importId) {
        return (root, query, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();

            predicates = criteriaBuilder.and(
                    predicates,
                    criteriaBuilder.equal(root.get("merchant").get("id"), merchantId));

            if (status != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.equal(root.get("status"), status));
            }

            if (paymentMethod != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.equal(root.get("paymentMethod"), paymentMethod));
            }

            if (fromDate != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.greaterThanOrEqualTo(root.get("settlementDate"), fromDate));
            }

            if (toDate != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.lessThanOrEqualTo(root.get("settlementDate"), toDate));
            }

            if (importId != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.equal(root.get("importBatch").get("id"), importId));
            }

            return predicates;
        };
    }
}
