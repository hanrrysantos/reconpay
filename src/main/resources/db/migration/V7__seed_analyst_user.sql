INSERT INTO users (id, name, email, password, role, active, created_at, updated_at)
VALUES (
    'a0000000-0000-4000-8000-000000000002',
    'Financial Analyst',
    'analyst@reconpay.local',
    '$2a$10$I1Wypy8Ptz.fnrjPhwESW.Ynhs9PR3cUWF7eRueUsJWgbEoezjAnW',
    'FINANCIAL_ANALYST',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;
