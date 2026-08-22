-- A run may only hold one item per external reference, so a retried or
-- concurrent execution cannot silently double up the same reference.
CREATE UNIQUE INDEX uk_reconciliation_items_run_external_reference
    ON reconciliation_items (reconciliation_run_id, external_reference);

-- Both foreign keys were unindexed, forcing a sequential scan on any delete
-- against the parent tables.
CREATE INDEX idx_reconciliation_items_internal_transaction_id
    ON reconciliation_items (internal_transaction_id);

CREATE INDEX idx_reconciliation_items_external_settlement_id
    ON reconciliation_items (external_settlement_id);

-- A global index over a seven-value enum is not selective enough for the
-- planner to use. Filtering always happens within a single item.
DROP INDEX IF EXISTS idx_reconciliation_discrepancies_type;

CREATE INDEX idx_reconciliation_discrepancies_item_type
    ON reconciliation_discrepancies (reconciliation_item_id, type);
