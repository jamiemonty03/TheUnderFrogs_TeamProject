DROP TABLE IF EXISTS clean_prices CASCADE;

CREATE TABLE clean_prices (
    symbol VARCHAR(10) NOT NULL REFERENCES instruments(symbol),

    date DATE NOT NULL,

    open NUMERIC(18, 4) NOT NULL, 
    high NUMERIC(18,4) NOT NULL,
    low NUMERIC(18,4) NOT NULL,
    close NUMERIC(18,4) NOT NULL,

    volume BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

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