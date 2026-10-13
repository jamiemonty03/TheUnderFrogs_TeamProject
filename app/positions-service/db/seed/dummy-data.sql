-- Dummy data for positions
INSERT INTO portfolios (account_id, name, risk, is_default)
SELECT account_id, 'Main', 'MEDIUM', TRUE
FROM (VALUES ('ACC0001'), ('ACC0002'), ('ACC0003'), ('ACC0004'), ('ACC0005'), ('ACC0006'),
             ('ACC0007'), ('ACC0008'), ('ACC0009'), ('ACC0010'), ('ACC0011')) AS seeded(account_id)
ORDER BY account_id;

INSERT INTO positions (account_id, portfolio_id, symbol, quantity, average_cost, version, created_at, last_updated, updated_by)
SELECT h.account_id, p.portfolio_id, h.symbol, h.quantity, h.average_cost, 0, NOW(), NOW(), 'SYSTEM'
FROM (VALUES
    -- ACC0001 (Alice Johnson)
    ('ACC0001', 'AAPL',  10.0000, 190.25),
    ('ACC0001', 'SPY',    5.0000, 445.00),
    -- ACC0002 (Bob Smith)
    ('ACC0002', 'MSFT',   5.0000, 410.10),
    ('ACC0002', 'JPM',    8.0000, 156.75),
    -- ACC0003 (Carla Diaz)
    ('ACC0003', 'AMZN',   6.0000, 182.40),
    -- ACC0004 (David Lee)
    ('ACC0004', 'TSLA',   4.0000, 250.35),
    -- ACC0005 (Emma Wilson)
    ('ACC0005', 'TSLA',  15.0000, 245.75),
    ('ACC0005', 'NVDA',   7.0000, 875.50),
    ('ACC0005', 'SPY',    3.0000, 443.50),
    -- ACC0006 (Frank Moore)
    ('ACC0006', 'KO',    10.0000,  61.80),
    -- ACC0007 (Grace Kim)
    ('ACC0007', 'EFA',   20.0000,  78.40),
    -- ACC0008 (Henry Chen)
    ('ACC0008', 'TLT',   50.0000,  95.40),
    ('ACC0008', 'AGG',   25.0000,  85.60),
    -- ACC0009 (Isla Brown)
    ('ACC0009', 'DIA',    2.0000, 385.90),
    ('ACC0009', 'GOOGL',  3.0000, 158.75),
    -- ACC0010 (Jack Turner)
    ('ACC0010', 'LQD',   15.0000,  98.20),
    ('ACC0010', 'IWM',    8.0000, 198.50)
) AS h(account_id, symbol, quantity, average_cost)
JOIN portfolios p ON p.account_id = h.account_id AND p.is_default;
