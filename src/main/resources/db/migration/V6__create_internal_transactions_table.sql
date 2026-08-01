CREATE TABLE internal_transactions (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    external_reference VARCHAR(100) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    expected_net_amount NUMERIC(19, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    installments INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    transaction_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_internal_transactions_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT chk_internal_transactions_amount_positive
        CHECK (amount > 0),
    CONSTRAINT chk_internal_transactions_installments_positive
        CHECK (installments >= 1)
);

CREATE UNIQUE INDEX uk_internal_transactions_merchant_external_reference
    ON internal_transactions (merchant_id, external_reference);

CREATE INDEX idx_internal_transactions_merchant_transaction_date
    ON internal_transactions (merchant_id, transaction_date);
