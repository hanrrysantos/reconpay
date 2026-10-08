CREATE TABLE period_locks (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL REFERENCES merchants (id),
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    run_id UUID NOT NULL REFERENCES reconciliation_runs (id),
    locked_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_period_locks_window UNIQUE (merchant_id, from_date, to_date)
);
