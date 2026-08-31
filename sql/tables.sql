DROP TABLE IF EXISTS accounts CASCADE;
DROP TABLE IF EXISTS instruments CASCADE;

CREATE TABLE accounts (
    id           SERIAL PRIMARY KEY,
    account_id    VARCHAR(32) NOT NULL UNIQUE,
    holder_name   VARCHAR(255) NOT NULL,
    cash_balance  NUMERIC(18, 2) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    version       INTEGER DEFAULT 0,
    last_updated  TIMESTAMP DEFAULT NOW(),
);

CREATE TABLE instruments (
    symbol      VARCHAR(10) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    asset_class VARCHAR(50) NOT NULL,
    currency    CHAR(3) NOT NULL,
    tradable    BOOLEAN NOT NULL
);