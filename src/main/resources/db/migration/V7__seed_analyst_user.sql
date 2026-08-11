INSERT INTO users (id, name, email, password, role, active, created_at, updated_at)
VALUES (
    'a0000000-0000-4000-8000-000000000002',
    'Financial Analyst',
    'user@gmail.com',
    '$2a$10$YQ3BdqlyJE1ds0aKxFGjf.0zpTCvP9FCU2XX/5ulMBxJ26286kGa2',
    'FINANCIAL_ANALYST',
    TRUE,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO NOTHING;
