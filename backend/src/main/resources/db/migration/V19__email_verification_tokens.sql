CREATE TABLE email_verification_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_email_verification_tokens_user_id ON email_verification_tokens (user_id);

CREATE UNIQUE INDEX uq_email_verification_tokens_active_hash
    ON email_verification_tokens (token_hash)
    WHERE consumed_at IS NULL;
