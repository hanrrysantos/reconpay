CREATE TABLE fee_rules (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    installments INTEGER NOT NULL,
    fee_percentage NUMERIC(10,4) NOT NULL,
    fixed_fee NUMERIC(19,2) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_fee_rules_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id),

    CONSTRAINT uk_fee_rule_merchant_method_installments
        UNIQUE (merchant_id, payment_method, installments)
);