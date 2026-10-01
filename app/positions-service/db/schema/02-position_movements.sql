DROP TABLE IF EXISTS position_movements CASCADE;

CREATE TABLE position_movements (
    id             SERIAL PRIMARY KEY,
    order_id       VARCHAR(64) NOT NULL,
    movement_type  VARCHAR(20) NOT NULL CHECK (movement_type IN ('BUY', 'SELL', 'REVERSAL')),
    account_id     VARCHAR(32) NOT NULL,
    symbol         VARCHAR(10) NOT NULL,
    quantity       DECIMAL(18, 4) NOT NULL CHECK (quantity > 0),
    price          DECIMAL(18, 4) NOT NULL CHECK (price >= 0),
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_position_movements_order_type UNIQUE (order_id, movement_type)
);

CREATE INDEX idx_position_movements_account_symbol ON position_movements(account_id, symbol);
