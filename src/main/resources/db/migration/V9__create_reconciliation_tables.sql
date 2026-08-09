CREATE TABLE reconciliation_runs (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    from_date DATE,
    to_date DATE,
    total_items INTEGER NOT NULL,
    matched_count INTEGER NOT NULL,
    divergent_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_reconciliation_runs_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT chk_reconciliation_runs_total_items_non_negative
        CHECK (total_items >= 0),
    CONSTRAINT chk_reconciliation_runs_matched_count_non_negative
        CHECK (matched_count >= 0),
    CONSTRAINT chk_reconciliation_runs_divergent_count_non_negative
        CHECK (divergent_count >= 0)
);

CREATE INDEX idx_reconciliation_runs_merchant_created_at
    ON reconciliation_runs (merchant_id, created_at DESC);

CREATE TABLE reconciliation_items (
    id UUID PRIMARY KEY,
    reconciliation_run_id UUID NOT NULL,
    internal_transaction_id UUID,
    external_settlement_id UUID,
    external_reference VARCHAR(100) NOT NULL,
    result VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_reconciliation_items_run
        FOREIGN KEY (reconciliation_run_id) REFERENCES reconciliation_runs(id),
    CONSTRAINT fk_reconciliation_items_internal_transaction
        FOREIGN KEY (internal_transaction_id) REFERENCES internal_transactions(id),
    CONSTRAINT fk_reconciliation_items_external_settlement
        FOREIGN KEY (external_settlement_id) REFERENCES external_settlements(id)
);

CREATE INDEX idx_reconciliation_items_run_id
    ON reconciliation_items (reconciliation_run_id);

CREATE INDEX idx_reconciliation_items_run_result
    ON reconciliation_items (reconciliation_run_id, result);

CREATE TABLE reconciliation_discrepancies (
    id UUID PRIMARY KEY,
    reconciliation_item_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    expected_value VARCHAR(255),
    actual_value VARCHAR(255),

    CONSTRAINT fk_reconciliation_discrepancies_item
        FOREIGN KEY (reconciliation_item_id) REFERENCES reconciliation_items(id)
);

CREATE INDEX idx_reconciliation_discrepancies_item_id
    ON reconciliation_discrepancies (reconciliation_item_id);

CREATE INDEX idx_reconciliation_discrepancies_type
    ON reconciliation_discrepancies (type);
