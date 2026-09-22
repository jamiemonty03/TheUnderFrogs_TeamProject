-- account_id and symbol are owned by accounts-service / instruments-service; no cross-database FKs

DROP TABLE IF EXISTS client_trades CASCADE;

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

CREATE INDEX idx_client_trades_account_id ON client_trades(account_id);
CREATE INDEX idx_client_trades_symbol ON client_trades(symbol);
CREATE INDEX idx_client_trades_account_symbol ON client_trades(account_id, symbol);
CREATE INDEX idx_client_trades_trade_date ON client_trades(trade_date);