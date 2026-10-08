-- V5 and V7 seeded users with credentials committed to the repository and ran in
-- every profile, including production. The seeds now live in db/seed, which only
-- the dev and test profiles load. This removes the accounts those migrations created.
DELETE FROM users
WHERE id IN (
    'a0000000-0000-4000-8000-000000000001',
    'a0000000-0000-4000-8000-000000000002'
);
