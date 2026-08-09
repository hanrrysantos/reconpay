package br.com.hanrry.reconpay.reconciliation.repository;

import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class ReconciliationItemSpecifications {

    private ReconciliationItemSpecifications() {
    }

    public static Specification<ReconciliationItemEntity> withFilters(
            UUID runId,
            ReconciliationResult result,
            DiscrepancyType discrepancyType) {
        return (root, query, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();

            predicates = criteriaBuilder.and(
                    predicates,
                    criteriaBuilder.equal(root.get("reconciliationRun").get("id"), runId));

            if (result != null) {
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.equal(root.get("result"), result));
            }

            if (discrepancyType != null) {
                Join<Object, Object> discrepancies = root.join("discrepancies", JoinType.INNER);
                predicates = criteriaBuilder.and(
                        predicates,
                        criteriaBuilder.equal(discrepancies.get("type"), discrepancyType));
                query.distinct(true);
            }

            return predicates;
        };
    }
}
