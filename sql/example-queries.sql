-- Full order book — who placed what, on which instrument

SELECT
    a.holder_name,
    a.account_id,
    o.order_id,
    i.symbol,
    i.name        AS instrument_name,
    i.asset_class,
    o.side,
    o.quantity,
    o.price,
    (o.quantity * o.price) AS order_value,
    o.order_status,
    o.created_at
FROM orders o
JOIN accounts a    ON o.account_id = a.account_id
JOIN instruments i ON o.symbol = i.symbol
ORDER BY o.created_at;

-- Current portfolio positions per account

SELECT
    a.holder_name,
    a.account_id,
    i.symbol,
    i.name        AS instrument_name,
    i.asset_class,
    p.quantity,
    p.average_cost,
    (p.quantity * p.average_cost) AS position_value
FROM positions p
JOIN accounts a    ON p.account_id = a.account_id
JOIN instruments i ON p.symbol = i.symbol
ORDER BY position_value DESC;

-- One combined view — account summary with cash + holdings + recent orders 

SELECT
    a.holder_name,
    a.account_id,
    a.cash_balance,
    a.status       AS account_status,
    i.symbol,
    i.asset_class,
    p.quantity     AS position_qty,
    p.average_cost,
    o.order_status AS latest_order_status,
    o.created_at   AS latest_order_date
FROM accounts a
LEFT JOIN positions p ON a.account_id = p.account_id
LEFT JOIN instruments i ON p.symbol = i.symbol
LEFT JOIN orders o ON o.account_id = a.account_id AND o.symbol = i.symbol
ORDER BY a.holder_name, i.symbol;