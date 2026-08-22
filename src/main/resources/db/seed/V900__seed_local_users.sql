-- Loaded only by the dev and test profiles via spring.flyway.locations.
-- Passwords: DevAdmin@2026 and DevAnalyst@2026. Local use only.
INSERT INTO users (id, name, email, password, role, active, created_at, updated_at)
VALUES (
    'a0000000-0000-4000-8000-000000000101',
    'Administrator',
    'admin@reconpay.local',
    '$2y$10$ySResS9tz/HAhbGrrw747OtvudkndM1/T.HP4tQnppZP2Yw3Y7Ole',
    'ADMIN',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (id, name, email, password, role, active, created_at, updated_at)
VALUES (
    'a0000000-0000-4000-8000-000000000102',
    'Financial Analyst',
    'analyst@reconpay.local',
    '$2y$10$1JGn6gyzzdMpjsu7kac/NusrGjnxI8PtbFZM3zEXwDFjtM55/Grou',
    'FINANCIAL_ANALYST',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;
