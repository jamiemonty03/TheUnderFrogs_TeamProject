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
    side             VARCHAR(4) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    quantity         INT NOT NULL CHECK (quantity > 0),
    price            NUMERIC(18,2) NOT NULL CHECK (price > 0),
    order_status     VARCHAR(20) NOT NULL CHECK (order_status IN ('NEW', 'FILLED', 'CANCELLED', 'REJECTED')),
    created_at       TIMESTAMP DEFAULT NOW()
);

-- Indexes for query performance
CREATE INDEX idx_orders_account_id ON orders(account_id);
CREATE INDEX idx_orders_symbol ON orders(symbol);
CREATE INDEX idx_orders_order_status ON orders(order_status);

CREATE TABLE positions (
    account_id  VARCHAR(32) NOT NULL,
    symbol      VARCHAR(10) NOT NULL,
    quantity    DECIMAL(18, 4) NOT NULL,
    average_cost DECIMAL(18, 4) NOT NULL,
    PRIMARY KEY (account_id, symbol),
    FOREIGN KEY (account_id) REFERENCES accounts(account_id),
    FOREIGN KEY (symbol) REFERENCES instruments(symbol)
);