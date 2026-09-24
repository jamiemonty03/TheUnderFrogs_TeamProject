DROP TABLE IF EXISTS clean_stocks CASCADE;

CREATE TABLE clean_stocks (

    symbol VARCHAR(10) NOT NULL,

    sector VARCHAR(100) NOT NULL,
    industry VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,

    market_cap NUMERIC(20,2) NOT NULL,
    shares_outstanding NUMERIC(20,2) NOT NULL,

    full_time_employees INTEGER,

    trailing_pe NUMERIC(10,4),
    forward_pe NUMERIC(10,4),

    trailing_eps NUMERIC(10,4),

    dividend_rate NUMERIC(10,4),

    payout_ratio NUMERIC(10,4),

    price_to_book NUMERIC(10,4),

    return_on_equity NUMERIC(10,4),
    
    total_revenue NUMERIC(20,2),
    
    website VARCHAR(255),

    version INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    last_updated TIMESTAMP NOT NULL DEFAULT NOW(),

    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',

    PRIMARY KEY (symbol),

    FOREIGN KEY (symbol) REFERENCES instruments(symbol) ON DELETE CASCADE,

    CHECK (market_cap >= 0),
    CHECK (shares_outstanding >= 0),
    CHECK (full_time_employees >= 0),
    CHECK (trailing_pe >= 0),
    CHECK (forward_pe >= 0),
    CHECK (dividend_rate >= 0),
    CHECK (payout_ratio >= 0),
    CHECK (price_to_book >= 0),
    CHECK (total_revenue >= 0)

);