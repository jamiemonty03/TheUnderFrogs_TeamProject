CREATE TEMP TABLE accounts_users (
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255),
    account_id    VARCHAR(32),
    is_active     BOOLEAN      NOT NULL,
    created_at    TIMESTAMP    NOT NULL
) ON COMMIT DROP;

\copy accounts_users FROM pstdin WITH (FORMAT csv)

INSERT INTO users (username, email, password_hash, full_name, roles, account_id, is_active, created_at, updated_by)
SELECT username, email, password_hash, full_name, ARRAY['USER'], account_id, is_active, created_at, 'MIGRATION'
FROM accounts_users
ORDER BY username
ON CONFLICT DO NOTHING;

INSERT INTO auth (user_id)
SELECT u.id
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM auth a WHERE a.user_id = u.id);
