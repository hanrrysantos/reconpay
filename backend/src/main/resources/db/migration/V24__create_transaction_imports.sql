CREATE TABLE transaction_imports (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    total_rows INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_transaction_imports_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT chk_transaction_imports_total_rows_positive
        CHECK (total_rows > 0)
);

CREATE INDEX idx_transaction_imports_merchant_created_at
    ON transaction_imports (merchant_id, created_at DESC);

-- Transactions created one by one, and every transaction before this migration, have no batch.
ALTER TABLE internal_transactions
    ADD COLUMN import_id UUID,
    ADD CONSTRAINT fk_internal_transactions_import
        FOREIGN KEY (import_id) REFERENCES transaction_imports(id);

CREATE INDEX idx_internal_transactions_import
    ON internal_transactions (import_id);
