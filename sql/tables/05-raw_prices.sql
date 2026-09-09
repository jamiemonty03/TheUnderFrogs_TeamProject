DROP TABLE IF EXISTS raw_prices CASCADE;

CREATE TABLE raw_prices (
    symbol      VARCHAR(10) NOT NULL REFERENCES instruments(symbol),
    date        DATE NOT NULL,
    open        NUMERIC(18, 4) NOT NULL,
    high        NUMERIC(18, 4) NOT NULL,
    low         NUMERIC(18, 4) NOT NULL,
    close       NUMERIC(18, 4) NOT NULL,
    volume      BIGINT NOT NULL,
    PRIMARY KEY (symbol, date),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by  VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);

CREATE INDEX idx_raw_prices_symbol_date ON raw_prices(symbol, date);