DROP TABLE IF EXISTS instruments CASCADE;

CREATE TABLE instruments (
    symbol      VARCHAR(10) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    -- Asset class of the instrument (e.g., Equity, Bond, Commodity)
    asset_class VARCHAR(50) NOT NULL CHECK (asset_class IN ('Equity', 'Bond', 'Commodity')),
    currency    CHAR(3) NOT NULL,
    tradable    BOOLEAN NOT NULL DEFAULT FALSE,
    version       INTEGER NOT NULL DEFAULT 0,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated  TIMESTAMP  NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);
