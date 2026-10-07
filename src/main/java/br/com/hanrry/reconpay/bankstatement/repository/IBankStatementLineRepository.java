package br.com.hanrry.reconpay.bankstatement.repository;

import br.com.hanrry.reconpay.bankstatement.entity.BankStatementLineEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IBankStatementLineRepository extends JpaRepository<BankStatementLineEntity, UUID> {

    List<BankStatementLineEntity> findByMerchant_IdAndMovementDateBetween(
            UUID merchantId,
            LocalDate fromDate,
            LocalDate toDate);

    List<BankStatementLineEntity> findByMerchant_IdAndLineReferenceIn(
            UUID merchantId,
            Collection<String> lineReferences);

    Page<BankStatementLineEntity> findByMerchant_Id(UUID merchantId, Pageable pageable);

    Page<BankStatementLineEntity> findByMerchant_IdAndImportBatch_Id(
            UUID merchantId,
            UUID importId,
            Pageable pageable);
}
