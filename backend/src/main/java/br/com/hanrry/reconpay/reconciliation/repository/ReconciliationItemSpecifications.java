package br.com.hanrry.reconpay.reconciliation.repository;

import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationDiscrepancyEntity;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationItemEntity;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class ReconciliationItemSpecifications {

    private ReconciliationItemSpecifications() {
    }

    public static Specification<ReconciliationItemEntity> withFilters(
            UUID runId,
            ReconciliationResult result,
            DiscrepancyType discrepancyType,
            DiscrepancyStatus discrepancyStatus) {
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

            /*
             * EXISTS instead of join + distinct: an item with two matching
             * discrepancies still comes back once, and the query stays free
             * of DISTINCT, which PostgreSQL rejects alongside an ORDER BY on
             * an expression that is not in the select list. Type and status
             * apply to the same discrepancy, so "open fee divergence" means
             * one discrepancy that is both.
             */
            if (discrepancyType != null || discrepancyStatus != null) {
                Subquery<UUID> discrepancies = query.subquery(UUID.class);
                Root<ReconciliationDiscrepancyEntity> discrepancy =
                        discrepancies.from(ReconciliationDiscrepancyEntity.class);
                var matches = criteriaBuilder.equal(discrepancy.get("reconciliationItem"), root);
                if (discrepancyType != null) {
                    matches = criteriaBuilder.and(
                            matches,
                            criteriaBuilder.equal(discrepancy.get("type"), discrepancyType));
                }
                if (discrepancyStatus != null) {
                    matches = criteriaBuilder.and(
                            matches,
                            criteriaBuilder.equal(discrepancy.get("status"), discrepancyStatus));
                }
                discrepancies.select(discrepancy.get("id")).where(matches);
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.exists(discrepancies));
            }

            return predicates;
        };
    }
}
