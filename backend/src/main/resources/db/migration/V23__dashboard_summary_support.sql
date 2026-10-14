-- The bank matcher used to discard which statement line paid each settlement.
-- The dashboard summary needs the matched credit per item to total what the
-- bank actually received.
ALTER TABLE reconciliation_items
    ADD COLUMN bank_amount NUMERIC(19, 2),
    ADD COLUMN bank_movement_date DATE;

-- Runs processed before this migration never recorded bank credits, so the
-- summary must report their bank figures as unknown rather than zero.
ALTER TABLE reconciliation_runs
    ADD COLUMN bank_tracked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN processed_items INTEGER;
