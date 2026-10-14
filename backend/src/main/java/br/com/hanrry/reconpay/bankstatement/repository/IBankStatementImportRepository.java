package br.com.hanrry.reconpay.bankstatement.repository;

import br.com.hanrry.reconpay.bankstatement.entity.BankStatementImportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IBankStatementImportRepository extends JpaRepository<BankStatementImportEntity, UUID> {

    Page<BankStatementImportEntity> findAllByMerchant_Id(UUID merchantId, Pageable pageable);
}
