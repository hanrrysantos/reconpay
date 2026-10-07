CREATE TABLE bank_statement_imports (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    total_rows INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_bank_statement_imports_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT chk_bank_statement_imports_total_rows_positive
        CHECK (total_rows > 0)
);

CREATE INDEX idx_bank_statement_imports_merchant_created_at
    ON bank_statement_imports (merchant_id, created_at DESC);

CREATE TABLE bank_statement_lines (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    import_id UUID NOT NULL,
    line_reference VARCHAR(100) NOT NULL,
    external_reference VARCHAR(100),
    amount NUMERIC(19, 2) NOT NULL,
    movement_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_bank_statement_lines_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    CONSTRAINT fk_bank_statement_lines_import
        FOREIGN KEY (import_id) REFERENCES bank_statement_imports(id),
    CONSTRAINT chk_bank_statement_lines_amount_positive
        CHECK (amount > 0)
);

CREATE UNIQUE INDEX uk_bank_statement_lines_merchant_line_reference
    ON bank_statement_lines (merchant_id, line_reference);

CREATE INDEX idx_bank_statement_lines_merchant_movement_date
    ON bank_statement_lines (merchant_id, movement_date);

CREATE INDEX idx_bank_statement_lines_import_id
    ON bank_statement_lines (import_id);
