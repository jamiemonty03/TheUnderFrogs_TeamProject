DROP TABLE IF EXISTS service_clients CASCADE;

CREATE TABLE service_clients (
    id                 BIGSERIAL PRIMARY KEY,
    client_id          VARCHAR(100) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(255) NOT NULL,
    roles              TEXT[]       NOT NULL DEFAULT ARRAY['SERVICE'],
    is_active          BOOLEAN      NOT NULL DEFAULT true,
    version            INT          NOT NULL DEFAULT 0,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by          VARCHAR(100)
);
