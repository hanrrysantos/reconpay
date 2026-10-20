-- A non-null expiry marks a temporary demo account; regular accounts keep it null.
ALTER TABLE users
    ADD COLUMN demo_expires_at TIMESTAMPTZ,
    ADD COLUMN demo_import_count INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_users_demo_expires_at
    ON users (demo_expires_at)
    WHERE demo_expires_at IS NOT NULL;
