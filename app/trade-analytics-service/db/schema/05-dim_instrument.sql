CREATE SCHEMA IF NOT EXISTS analytics;

CREATE TABLE IF NOT EXISTS analytics.DIM_INSTRUMENT (
    instrument_key SERIAL PRIMARY KEY,
    symbol VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    asset_class VARCHAR(50),
    currency CHAR(3),
    exchange VARCHAR(50),
    tradable BOOLEAN DEFAULT FALSE,
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    last_updated TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dim_instrument_symbol ON analytics.DIM_INSTRUMENT(symbol);
CREATE INDEX IF NOT EXISTS idx_dim_instrument_asset_class ON analytics.DIM_INSTRUMENT(asset_class);
