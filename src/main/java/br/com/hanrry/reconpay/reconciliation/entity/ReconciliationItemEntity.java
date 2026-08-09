package br.com.hanrry.reconpay.reconciliation.entity;

import br.com.hanrry.reconpay.externalsettlement.entity.ExternalSettlementEntity;
import br.com.hanrry.reconpay.reconciliation.enums.ReconciliationResult;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reconciliation_items")
public class ReconciliationItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_run_id", nullable = false)
    private ReconciliationRunEntity reconciliationRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_transaction_id")
    private InternalTransactionEntity internalTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_settlement_id")
    private ExternalSettlementEntity externalSettlement;

    @Column(name = "external_reference", nullable = false, length = 100)
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReconciliationResult result;

    @OneToMany(mappedBy = "reconciliationItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReconciliationDiscrepancyEntity> discrepancies = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public void addDiscrepancy(ReconciliationDiscrepancyEntity discrepancy) {
        discrepancies.add(discrepancy);
        discrepancy.setReconciliationItem(this);
    }
}
