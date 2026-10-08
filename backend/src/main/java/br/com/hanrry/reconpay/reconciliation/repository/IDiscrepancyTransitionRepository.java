package br.com.hanrry.reconpay.reconciliation.repository;

import br.com.hanrry.reconpay.reconciliation.entity.DiscrepancyTransitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IDiscrepancyTransitionRepository extends JpaRepository<DiscrepancyTransitionEntity, UUID> {
}
