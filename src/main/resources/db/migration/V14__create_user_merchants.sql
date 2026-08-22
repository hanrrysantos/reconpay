-- Any authenticated analyst could read every merchant's transactions,
-- settlements and reconciliations. Access is now an explicit grant. An analyst
-- typically serves more than one merchant, so this is a join table rather than
-- a column on users.
CREATE TABLE user_merchants (
    user_id UUID NOT NULL,
    merchant_id UUID NOT NULL,
    granted_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_user_merchants PRIMARY KEY (user_id, merchant_id),
    CONSTRAINT fk_user_merchants_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_merchants_merchant
        FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_merchants_merchant_id
    ON user_merchants (merchant_id);
