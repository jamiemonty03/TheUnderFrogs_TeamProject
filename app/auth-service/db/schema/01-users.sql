DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'user',
    is_active BOOLEAN NOT NULL DEFAULT true,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_active ON users(is_active);

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
