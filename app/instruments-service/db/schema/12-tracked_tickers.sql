DROP TABLE IF EXISTS tracked_tickers CASCADE;

-- The ETL's source of truth for which symbols to fetch from yfinance.
-- Deliberately no FK to instruments: a ticker is tracked before its first
-- ETL run creates the instrument, and a deactivated row outlives the
-- instrument's deletion as the record that it was removed on purpose.
CREATE TABLE tracked_tickers (
    symbol      VARCHAR(10) PRIMARY KEY,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    added_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    added_by    VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);

INSERT INTO tracked_tickers (symbol) VALUES
    -- Stocks
    ('AAPL'), ('MSFT'), ('JPM'), ('TSLA'), ('GOOGL'),
    ('AMZN'), ('NVDA'), ('XOM'), ('JNJ'), ('KO'),
    -- ETFs
    ('SPY'), ('QQQ'), ('VTI'), ('IWM'), ('DIA'),
    ('EFA'), ('EEM'), ('XLF'), ('XLK'), ('XLE'),
    -- Bonds
    ('AGG'), ('TLT'), ('LQD'), ('BND'), ('SHY'),
    ('IEF'), ('HYG'), ('MUB'), ('TIP'), ('BNDX');
