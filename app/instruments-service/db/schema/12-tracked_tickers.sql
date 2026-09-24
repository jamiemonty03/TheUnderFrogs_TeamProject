DROP TABLE IF EXISTS tracked_tickers CASCADE;

-- The ETL's source of truth for which symbols to fetch from yfinance.
-- Deliberately no FK to instruments: a ticker is tracked before its first
-- ETL run creates the instrument, and a deactivated row outlives the
-- instrument's deletion as the record that it was removed on purpose.
CREATE TABLE tracked_tickers (
    symbol        VARCHAR(10) PRIMARY KEY,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    version       INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);
