CREATE TABLE orders (
    order_id CHAR(36) PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    account_id VARCHAR(32) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    side VARCHAR(4) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price_limit NUMERIC(18, 2) NOT NULL CHECK (price_limit > 0),
    order_status VARCHAR(20) NOT NULL CHECK (order_status IN ('NEW', 'FILLED', 'CANCELLED', 'REJECTED')),
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);

CREATE TABLE client_trades (
    trade_id     SERIAL PRIMARY KEY,
    account_id   VARCHAR(32) NOT NULL,
    symbol       VARCHAR(20) NOT NULL,
    trade_type   VARCHAR(4) NOT NULL CHECK (trade_type IN ('BUY', 'SELL')),
    quantity     NUMERIC(18,4) NOT NULL CHECK (quantity > 0),
    price        NUMERIC(18,2) NOT NULL CHECK (price > 0),
    trade_date   DATE NOT NULL,
    version      INTEGER NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by   VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);
