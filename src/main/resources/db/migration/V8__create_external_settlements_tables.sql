CREATE TABLE settlement_imports (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    total_rows INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_settlement_imports_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT chk_settlement_imports_total_rows_positive
        CHECK (total_rows > 0)
);

CREATE INDEX idx_settlement_imports_merchant_created_at
    ON settlement_imports (merchant_id, created_at DESC);

CREATE TABLE external_settlements (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    import_id UUID NOT NULL,
    external_reference VARCHAR(100) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    net_amount NUMERIC(19, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    installments INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    settlement_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_external_settlements_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_external_settlements_import
        FOREIGN KEY (import_id) REFERENCES settlement_imports(id),
    CONSTRAINT chk_external_settlements_amount_positive
        CHECK (amount > 0),
    CONSTRAINT chk_external_settlements_net_amount_positive
        CHECK (net_amount > 0),
    CONSTRAINT chk_external_settlements_installments_positive
        CHECK (installments >= 1)
);

CREATE UNIQUE INDEX uk_external_settlements_merchant_external_reference
    ON external_settlements (merchant_id, external_reference);

CREATE INDEX idx_external_settlements_merchant_settlement_date
    ON external_settlements (merchant_id, settlement_date);

CREATE INDEX idx_external_settlements_import_id
    ON external_settlements (import_id);
