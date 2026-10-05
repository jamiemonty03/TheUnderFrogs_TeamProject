DROP TABLE IF EXISTS auth CASCADE;

CREATE TABLE auth (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_login VARCHAR(255),
    is_2fa_enabled BOOLEAN NOT NULL DEFAULT false,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auth_user_id ON auth(user_id);
CREATE INDEX idx_auth_is_2fa_enabled ON auth(is_2fa_enabled);

-- Seed data: auth records for test users
INSERT INTO auth (user_id, is_2fa_enabled, failed_login_attempts, created_at, updated_at)
VALUES (1, false, 0, NOW(), NOW());

INSERT INTO auth (user_id, is_2fa_enabled, failed_login_attempts, created_at, updated_at)
VALUES (2, false, 0, NOW(), NOW());
