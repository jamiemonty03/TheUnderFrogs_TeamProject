DROP TABLE IF EXISTS cash_movements CASCADE;

CREATE TABLE cash_movements (
    id             SERIAL PRIMARY KEY,
    order_id       VARCHAR(64) NOT NULL,
    movement_type  VARCHAR(20) NOT NULL CHECK (movement_type IN ('DEBIT', 'CREDIT', 'REVERSAL')),
    account_id     VARCHAR(32) NOT NULL,
    amount         NUMERIC(18, 2) NOT NULL CHECK (amount > 0),
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cash_movements_order_type UNIQUE (order_id, movement_type),
    CONSTRAINT fk_cash_movements_accounts FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

CREATE INDEX idx_cash_movements_account_id ON cash_movements(account_id);
