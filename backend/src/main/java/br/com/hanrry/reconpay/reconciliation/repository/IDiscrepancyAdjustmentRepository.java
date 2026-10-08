package br.com.hanrry.reconpay.reconciliation.repository;

import br.com.hanrry.reconpay.reconciliation.entity.DiscrepancyAdjustmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IDiscrepancyAdjustmentRepository extends JpaRepository<DiscrepancyAdjustmentEntity, UUID> {
}
