-- Items previously stored only foreign keys, so mutating a transaction or
-- re-importing a settlement rewrote the contents of past runs while the
-- aggregate counters on reconciliation_runs stayed frozen. Snapshotting both
-- sides at run time makes a run immutable evidence of what was compared.
ALTER TABLE reconciliation_items
    ADD COLUMN transaction_amount NUMERIC(19, 2),
    ADD COLUMN expected_net_amount NUMERIC(19, 2),
    ADD COLUMN transaction_payment_method VARCHAR(30),
    ADD COLUMN transaction_installments INTEGER,
    ADD COLUMN transaction_status VARCHAR(30),
    ADD COLUMN transaction_date DATE,
    ADD COLUMN settlement_amount NUMERIC(19, 2),
    ADD COLUMN settlement_net_amount NUMERIC(19, 2),
    ADD COLUMN settlement_payment_method VARCHAR(30),
    ADD COLUMN settlement_installments INTEGER,
    ADD COLUMN settlement_status VARCHAR(30),
    ADD COLUMN settlement_date DATE;

UPDATE reconciliation_items item
SET transaction_amount = txn.amount,
    expected_net_amount = txn.expected_net_amount,
    transaction_payment_method = txn.payment_method,
    transaction_installments = txn.installments,
    transaction_status = txn.status,
    transaction_date = txn.transaction_date
FROM internal_transactions txn
WHERE txn.id = item.internal_transaction_id;

UPDATE reconciliation_items item
SET settlement_amount = settlement.amount,
    settlement_net_amount = settlement.net_amount,
    settlement_payment_method = settlement.payment_method,
    settlement_installments = settlement.installments,
    settlement_status = settlement.status,
    settlement_date = settlement.settlement_date
FROM external_settlements settlement
WHERE settlement.id = item.external_settlement_id;
