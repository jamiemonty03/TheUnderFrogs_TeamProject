DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255),
    roles           TEXT[]       NOT NULL DEFAULT ARRAY['TRADER'],
    account_id      VARCHAR(32)  UNIQUE,
    is_active       BOOLEAN      NOT NULL DEFAULT true,
    failed_attempts INTEGER      NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
    locked_until    TIMESTAMP,
    version         INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by      VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);

CREATE INDEX idx_users_is_active ON users(is_active);
