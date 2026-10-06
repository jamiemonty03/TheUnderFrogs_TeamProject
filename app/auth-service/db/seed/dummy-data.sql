INSERT INTO users (username, email, password_hash, full_name, roles)
VALUES
    ('admin',    'admin@example.com',    '$2a$10$w1j2AzxjbJhzrMeHbKQCeOV1rUqvTTJdM5L.Jzw6pJ7.1XQvPQvLK', 'Admin',     ARRAY['ADMIN']),
    ('testuser', 'testuser@example.com', '$2a$10$w1j2AzxjbJhzrMeHbKQCeOV1rUqvTTJdM5L.Jzw6pJ7.1XQvPQvLK', 'Test User', ARRAY['USER']);

INSERT INTO auth (user_id, is_2fa_enabled, failed_login_attempts, created_at, updated_at)
SELECT id, false, 0, NOW(), NOW()
FROM users
WHERE username IN ('admin', 'testuser');
