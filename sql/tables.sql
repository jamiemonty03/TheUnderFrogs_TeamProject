
CREATE TABLE instruments (
    symbol      VARCHAR(10) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    asset_class VARCHAR(50) NOT NULL,
    currency    CHAR(3) NOT NULL,
    tradable    BOOLEAN NOT NULL
);