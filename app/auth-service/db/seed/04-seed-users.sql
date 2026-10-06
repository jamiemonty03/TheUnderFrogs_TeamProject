-- Seed data: test users
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
