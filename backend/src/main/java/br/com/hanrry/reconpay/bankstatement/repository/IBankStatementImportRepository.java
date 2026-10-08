package br.com.hanrry.reconpay.bankstatement.repository;

import br.com.hanrry.reconpay.bankstatement.entity.BankStatementImportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IBankStatementImportRepository extends JpaRepository<BankStatementImportEntity, UUID> {
}
