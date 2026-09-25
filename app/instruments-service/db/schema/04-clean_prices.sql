DROP TABLE IF EXISTS clean_prices CASCADE;

CREATE TABLE clean_prices (
    symbol VARCHAR(10) NOT NULL REFERENCES instruments(symbol) ON DELETE CASCADE,

    date DATE NOT NULL,

    open NUMERIC(18, 4) NOT NULL, 
    high NUMERIC(18,4) NOT NULL,
    low NUMERIC(18,4) NOT NULL,
    close NUMERIC(18,4) NOT NULL,

    volume BIGINT NOT NULL,

    version INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    last_updated TIMESTAMP NOT NULL DEFAULT NOW(),

    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',

    PRIMARY KEY (symbol, date),

    CHECK (open > 0),
    CHECK (high > 0),
    CHECK (low > 0),
    CHECK (close > 0),
    
    CHECK (high >= low),

    CHECK (high >= open),
    CHECK (high >= close),

    CHECK (low <= open),
    CHECK (low <= close),

    CHECK (volume >= 0)
);