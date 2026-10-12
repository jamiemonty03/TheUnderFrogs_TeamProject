DROP TABLE IF EXISTS tokens CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token_hash  CHAR(64)  NOT NULL UNIQUE,
    user_id     BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id   UUID      NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    revoked_at  TIMESTAMP,
    replaced_by BIGINT    REFERENCES refresh_tokens(id) ON DELETE SET NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
