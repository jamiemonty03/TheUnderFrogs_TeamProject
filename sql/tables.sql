DROP TABLE IF EXISTS accounts CASCADE;
DROP TABLE IF EXISTS instruments CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS positions CASCADE;

CREATE TABLE accounts (
    id           SERIAL PRIMARY KEY,
    account_id    VARCHAR(32) NOT NULL UNIQUE,
    holder_name   VARCHAR(255) NOT NULL,
    cash_balance  NUMERIC(18, 2) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    version       INTEGER DEFAULT 0,
    last_updated  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE instruments (
    symbol      VARCHAR(10) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    asset_class VARCHAR(50) NOT NULL,
    currency    CHAR(3) NOT NULL,
    tradable    BOOLEAN NOT NULL
);

CREATE TABLE orders (
    order_id         CHAR(36) NOT NULL PRIMARY KEY,
    account_id       VARCHAR(32) NOT NULL REFERENCES accounts(account_id),
    symbol           VARCHAR(20)  NOT NULL REFERENCES instruments(symbol),
    side             VARCHAR(4) NOT NULL,
    quantity         INT NOT NULL,
    price            NUMERIC(18,2) NOT NULL,
    order_status     VARCHAR(20) NOT NULL,
    idempotency_key  VARCHAR(100) UNIQUE,
    created_at       TIMESTAMP DEFAULT NOW()
);

CREATE TABLE positions (
    account_id  VARCHAR(32) NOT NULL,
    symbol      VARCHAR(10) NOT NULL,
    quantity    DECIMAL(18, 4) NOT NULL,
    average_cost DECIMAL(18, 4) NOT NULL,
    PRIMARY KEY (account_id, symbol),
    FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    FOREIGN KEY (symbol) REFERENCES instruments(symbol)
);