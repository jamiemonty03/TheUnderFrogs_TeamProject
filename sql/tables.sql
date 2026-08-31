
CREATE TABLE instruments (
    symbol      VARCHAR(10) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    asset_class VARCHAR(50) NOT NULL,
    currency    CHAR(3) NOT NULL,
    tradable    BOOLEAN NOT NULL
);

CREATE TABLE orders (
    order_id         CHAR(36) NOT NULL PRIMARY KEY,
    account_id       BIGINT NOT NULL REFERENCES accounts(account_id),
    symbol           VARCHAR(20)  NOT NULL REFERENCES instruments(symbol),
    side             VARCHAR(4) NOT NULL,
    quantity         INT NOT NULL,
    price            INT NOT NULL,
    order_status     VARCHAR(20) NOT NULL,
    idempotency_key  VARCHAR(100) UNIQUE
);