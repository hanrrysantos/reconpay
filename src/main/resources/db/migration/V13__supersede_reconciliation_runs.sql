-- Each POST used to create a brand new run with a full set of items, so a
-- client retry after a timeout silently duplicated a very expensive operation
-- and there was no way to tell which run represented the current state of a
-- period. Superseding keeps the history while leaving exactly one live run per
-- window; the partial unique index makes concurrent duplicates fail loudly
-- instead of both landing.
ALTER TABLE reconciliation_runs
    ADD COLUMN superseded_at TIMESTAMP;

CREATE UNIQUE INDEX uk_reconciliation_runs_current_window
    ON reconciliation_runs (merchant_id, from_date, to_date)
    WHERE superseded_at IS NULL;
