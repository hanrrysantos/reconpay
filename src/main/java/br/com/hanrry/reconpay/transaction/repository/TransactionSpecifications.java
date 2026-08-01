package br.com.hanrry.reconpay.transaction.repository;

import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<InternalTransactionEntity> withFilters(
            UUID merchantId,
            TransactionStatus status,
            PaymentMethod paymentMethod,
            LocalDate fromDate,
            LocalDate toDate) {
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
                        criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), fromDate));
            }

            if (toDate != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), toDate));
            }

            return predicates;
        };
    }
}
