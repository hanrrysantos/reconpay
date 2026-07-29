INSERT INTO users (id, name, email, password, role, active, created_at, updated_at)
VALUES (
    'a0000000-0000-4000-8000-000000000001',
    'Administrator',
    'admin@reconpay.local',
    '$2y$10$JExHkBQPbl3ynRTt42KoZuxAsyVM0i09xGksMSN8HqlVgcp.EPePO',
    'ADMIN',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;
