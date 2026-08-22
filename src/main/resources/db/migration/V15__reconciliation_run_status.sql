-- A run used to be produced entirely inside the POST, so its mere existence
-- meant "finished successfully". Execution is now asynchronous and the row is
-- created before the work starts, so the outcome has to be explicit.
ALTER TABLE reconciliation_runs
    ADD COLUMN status VARCHAR(20),
    ADD COLUMN started_at TIMESTAMP,
    ADD COLUMN finished_at TIMESTAMP,
    ADD COLUMN error_message TEXT;

UPDATE reconciliation_runs
SET status = 'COMPLETED',
    started_at = created_at,
    finished_at = created_at
WHERE status IS NULL;

ALTER TABLE reconciliation_runs
    ALTER COLUMN status SET NOT NULL,
    ADD CONSTRAINT ck_reconciliation_runs_status
        CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED'));

-- V13 assumed every persisted run was a finished one. A pending or failed run
-- must not occupy the "current result for this window" slot.
DROP INDEX IF EXISTS uk_reconciliation_runs_current_window;

CREATE UNIQUE INDEX uk_reconciliation_runs_current_window
    ON reconciliation_runs (merchant_id, from_date, to_date)
    WHERE status = 'COMPLETED' AND superseded_at IS NULL;

-- Two concurrent requests for the same window would both scan the same data and
-- race to supersede each other. The second one is rejected instead.
CREATE UNIQUE INDEX uk_reconciliation_runs_in_flight
    ON reconciliation_runs (merchant_id, from_date, to_date)
    WHERE status IN ('PENDING', 'RUNNING');

CREATE INDEX idx_reconciliation_runs_status
    ON reconciliation_runs (status)
    WHERE status IN ('PENDING', 'RUNNING');
