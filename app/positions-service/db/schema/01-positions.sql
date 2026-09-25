-- account_id and symbol are owned by accounts-service / instruments-service; no cross-database FKs

DROP TABLE IF EXISTS positions CASCADE;

CREATE TABLE positions (
    account_id  VARCHAR(32) NOT NULL,
    symbol      VARCHAR(10) NOT NULL,
    quantity    DECIMAL(18, 4) NOT NULL CHECK (quantity >= 0),
    average_cost DECIMAL(18, 4) NOT NULL CHECK (average_cost >= 0),
    PRIMARY KEY (account_id, symbol),
    version       INTEGER NOT NULL DEFAULT 0,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated  TIMESTAMP  NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);
