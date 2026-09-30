-- Analytics Schema for Trade Data Warehouse
-- Creates star schema for FACT_TRADES with dimensions and supporting tables
-- Run this script on instruments_db to set up the analytics layer

CREATE SCHEMA IF NOT EXISTS analytics;

-- etl_watermark: Tracks the high watermark for incremental loads
-- Stores the last successfully processed created_at timestamp from orders
CREATE TABLE IF NOT EXISTS analytics.etl_watermark (
    watermark_id SERIAL PRIMARY KEY,
    high_watermark TIMESTAMP NOT NULL,
    batch_id VARCHAR(50) NOT NULL,
    batch_start_time TIMESTAMP NOT NULL,
    batch_end_time TIMESTAMP NOT NULL,
    row_count_loaded INT DEFAULT 0,
    row_count_dead_lettered INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

-- etl_dead_letter: Audit table for rows that fail quality validation
-- Captures invalid rows with context for investigation and replay
CREATE TABLE IF NOT EXISTS analytics.etl_dead_letter (
    dead_letter_id SERIAL PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL,
    source_table VARCHAR(50) NOT NULL,
    source_row JSONB NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dead_letter_batch_id ON analytics.etl_dead_letter(batch_id);
CREATE INDEX IF NOT EXISTS idx_dead_letter_source_table ON analytics.etl_dead_letter(source_table);

-- DIM_DATE: Date dimension for time-based reporting
-- Populated with all dates from orders and can be extended as needed
CREATE TABLE IF NOT EXISTS analytics.DIM_DATE (
    date_key INT PRIMARY KEY,
    date_value DATE NOT NULL UNIQUE,
    year INT NOT NULL,
    quarter INT NOT NULL,
    month INT NOT NULL,
    day INT NOT NULL,
    day_of_week INT NOT NULL,
    week_of_year INT NOT NULL,
    day_name VARCHAR(20),
    month_name VARCHAR(20),
    is_weekend BOOLEAN,
    created_at TIMESTAMP DEFAULT NOW(),
    last_updated TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dim_date_value ON analytics.DIM_DATE(date_value);

-- DIM_ACCOUNT: Account dimension from accounts-db
-- Stores account attributes for slicing and dicing trades
CREATE TABLE IF NOT EXISTS analytics.DIM_ACCOUNT (
    account_key SERIAL PRIMARY KEY,
    account_id VARCHAR(32) NOT NULL UNIQUE,
    holder_name VARCHAR(255) NOT NULL,
    cash_balance NUMERIC(18, 2),
    account_status VARCHAR(20),
    user_id BIGINT,
    version INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    last_updated TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dim_account_id ON analytics.DIM_ACCOUNT(account_id);
CREATE INDEX IF NOT EXISTS idx_dim_account_status ON analytics.DIM_ACCOUNT(account_status);

-- DIM_INSTRUMENT: Instrument dimension from instruments-db
-- Stores instrument attributes for trade classification
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

-- FACT_TRADES: Central fact table for all trades
-- trade_key is order_id and serves as PK for merge operations (upsert on status changes)
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


