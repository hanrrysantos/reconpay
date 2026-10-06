package br.com.hanrry.reconpay.reconciliation.entity;

import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyType;
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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reconciliation_discrepancies")
public class ReconciliationDiscrepancyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_item_id", nullable = false)
    private ReconciliationItemEntity reconciliationItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DiscrepancyType type;

    @Column(name = "expected_value", length = 255)
    private String expectedValue;

    @Column(name = "actual_value", length = 255)
    private String actualValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DiscrepancyStatus status = DiscrepancyStatus.OPEN;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "discrepancy", fetch = FetchType.LAZY)
    private List<DiscrepancyAdjustmentEntity> adjustments = new ArrayList<>();

    @OneToMany(mappedBy = "discrepancy", fetch = FetchType.LAZY)
    private List<DiscrepancyTransitionEntity> transitions = new ArrayList<>();
}
