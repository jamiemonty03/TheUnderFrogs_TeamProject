DROP TABLE IF EXISTS raw_stocks CASCADE;


CREATE TABLE raw_stocks (
    symbol               VARCHAR(10) PRIMARY KEY REFERENCES instruments(symbol),
    sector               VARCHAR(100),
    industry             VARCHAR(100),
    country              VARCHAR(100),
    market_cap           NUMERIC(20, 2),
    shares_outstanding   NUMERIC(20, 2),
    full_time_employees  INTEGER,
    beta                 NUMERIC(10, 4),
    trailing_pe          NUMERIC(10, 4),
    forward_pe           NUMERIC(10, 4),
    trailing_eps         NUMERIC(10, 4),
    dividend_rate        NUMERIC(10, 4),
    payout_ratio         NUMERIC(10, 4),
    price_to_book        NUMERIC(10, 4),
    return_on_equity     NUMERIC(10, 4),
    total_revenue        NUMERIC(20, 2),
    website              VARCHAR(255),
    version       INTEGER NOT NULL DEFAULT 0,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated  TIMESTAMP  NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);