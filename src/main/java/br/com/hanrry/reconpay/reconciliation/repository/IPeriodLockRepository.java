package br.com.hanrry.reconpay.reconciliation.repository;

import br.com.hanrry.reconpay.reconciliation.entity.PeriodLockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPeriodLockRepository extends JpaRepository<PeriodLockEntity, UUID> {

    Optional<PeriodLockEntity> findByMerchant_IdAndFromDateAndToDate(
            UUID merchantId,
            LocalDate fromDate,
            LocalDate toDate);

    List<PeriodLockEntity> findByMerchant_Id(UUID merchantId);
}
