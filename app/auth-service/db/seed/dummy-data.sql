
INSERT INTO users (username, email, password, role, is_active, created_at, updated_at)
VALUES (
    'admin',
    'admin@example.com',
    '$2a$10$w1j2AzxjbJhzrMeHbKQCeOV1rUqvTTJdM5L.Jzw6pJ7.1XQvPQvLK', -- hashed 'Admin@123'
    'admin',
    true,
    NOW(),
    NOW()
);

INSERT INTO users (username, email, password, role, is_active, created_at, updated_at)
VALUES (
    'testuser',
    'testuser@example.com',
    '$2a$10$w1j2AzxjbJhzrMeHbKQCeOV1rUqvTTJdM5L.Jzw6pJ7.1XQvPQvLK', -- hashed 'Test@123'
    'user',
    true,
    NOW(),
    NOW()
);

INSERT INTO auth (user_id, is_2fa_enabled, failed_login_attempts, created_at, updated_at)
SELECT id, false, 0, NOW(), NOW()
FROM users
WHERE username IN ('admin', 'testuser');
