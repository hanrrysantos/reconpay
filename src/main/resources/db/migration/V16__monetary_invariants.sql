-- NOT VALID preserves historical rows that need operator review while enforcing
-- the invariant for every new/updated row. Validate after legacy data is repaired.
ALTER TABLE fee_rules
    ADD CONSTRAINT ck_fee_rules_percentage_range
        CHECK (fee_percentage BETWEEN 0 AND 100) NOT VALID,
    ADD CONSTRAINT ck_fee_rules_fixed_fee_nonnegative
        CHECK (fixed_fee >= 0) NOT VALID;

ALTER TABLE internal_transactions
    ADD CONSTRAINT ck_internal_transactions_expected_net_positive
        CHECK (expected_net_amount > 0) NOT VALID;
