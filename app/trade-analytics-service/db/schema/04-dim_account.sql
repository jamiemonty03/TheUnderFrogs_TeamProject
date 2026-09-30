CREATE SCHEMA IF NOT EXISTS analytics;

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
