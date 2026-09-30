CREATE SCHEMA IF NOT EXISTS analytics;

CREATE TABLE IF NOT EXISTS analytics.FACT_TRADES (
    trade_key CHAR(36) PRIMARY KEY,
    account_key INT NOT NULL REFERENCES analytics.DIM_ACCOUNT(account_key),
    instrument_key INT NOT NULL REFERENCES analytics.DIM_INSTRUMENT(instrument_key),
    date_key INT NOT NULL REFERENCES analytics.DIM_DATE(date_key),
    side VARCHAR(4) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    quantity INT NOT NULL CHECK (quantity > 0),
    price NUMERIC(18, 2) NOT NULL CHECK (price > 0),
    order_status VARCHAR(20) NOT NULL CHECK (order_status IN ('NEW', 'FILLED', 'REJECTED', 'CANCELLED')),
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    last_updated TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fact_trades_account_key ON analytics.FACT_TRADES(account_key);
CREATE INDEX IF NOT EXISTS idx_fact_trades_instrument_key ON analytics.FACT_TRADES(instrument_key);
CREATE INDEX IF NOT EXISTS idx_fact_trades_date_key ON analytics.FACT_TRADES(date_key);
CREATE INDEX IF NOT EXISTS idx_fact_trades_status ON analytics.FACT_TRADES(order_status);
CREATE INDEX IF NOT EXISTS idx_fact_trades_created_at ON analytics.FACT_TRADES(created_at);
