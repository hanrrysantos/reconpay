ALTER TABLE reconciliation_discrepancies
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'OPEN';

ALTER TABLE reconciliation_discrepancies
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE discrepancy_adjustments (
    id UUID PRIMARY KEY,
    discrepancy_id UUID NOT NULL REFERENCES reconciliation_discrepancies(id),
    amount NUMERIC(19,2) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL,
    voided_at TIMESTAMP NULL,
    CONSTRAINT ck_discrepancy_adjustments_amount_nonzero CHECK (amount <> 0)
);

CREATE UNIQUE INDEX uk_discrepancy_adjustments_one_active
    ON discrepancy_adjustments (discrepancy_id)
    WHERE voided_at IS NULL;

CREATE TABLE discrepancy_transitions (
    id UUID PRIMARY KEY,
    discrepancy_id UUID NOT NULL REFERENCES reconciliation_discrepancies(id),
    actor_user_id UUID NOT NULL REFERENCES users(id),
    from_status VARCHAR(30) NOT NULL,
    to_status VARCHAR(30) NOT NULL,
    note VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_discrepancy_transitions_discrepancy_created
    ON discrepancy_transitions (discrepancy_id, created_at);
