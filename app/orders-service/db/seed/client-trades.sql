-- Historical trade data that nets to the current positions seed data.

INSERT INTO client_trades (account_id, symbol, trade_type, quantity, price, trade_date, version, created_at, last_updated, updated_by) VALUES
-- ACC0001 (Alice Johnson)
('ACC0001', 'AAPL',  'BUY',  15.0000, 188.00, '2026-01-08', 0, NOW(), NOW(), 'SYSTEM'),
('ACC0001', 'AAPL',  'SELL',  5.0000, 192.50, '2026-01-19', 0, NOW(), NOW(), 'SYSTEM'),
('ACC0001', 'SPY',   'BUY',   5.0000, 445.00, '2026-02-02', 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0002 (Bob Smith)
('ACC0002', 'MSFT',  'BUY',   5.0000, 410.10, '2026-02-06', 0, NOW(), NOW(), 'SYSTEM'),
('ACC0002', 'JPM',   'BUY',  10.0000, 154.20, '2026-02-09', 0, NOW(), NOW(), 'SYSTEM'),
('ACC0002', 'JPM',   'SELL',  2.0000, 156.75, '2026-02-21', 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0003 (Carla Diaz)
('ACC0003', 'AMZN',  'BUY',   6.0000, 182.40, '2026-03-03', 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0004 (David Lee)
('ACC0004', 'TSLA',  'BUY',   4.0000, 250.35, '2026-03-08', 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0005 (Emma Wilson)
('ACC0005', 'TSLA',  'BUY',  20.0000, 240.00, '2026-03-15', 0, NOW(), NOW(), 'SYSTEM'),
('ACC0005', 'TSLA',  'SELL',  5.0000, 245.75, '2026-04-04', 0, NOW(), NOW(), 'SYSTEM'),
('ACC0005', 'NVDA',  'BUY',   7.0000, 875.50, '2026-03-18', 0, NOW(), NOW(), 'SYSTEM'),
('ACC0005', 'SPY',   'BUY',   3.0000, 443.50, '2026-04-07', 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0006 (Frank Moore)
('ACC0006', 'KO',    'BUY',  10.0000,  61.80, '2026-04-11', 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0007 (Grace Kim)
('ACC0007', 'EFA',   'BUY',  20.0000,  78.40, '2026-04-13', 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0008 (Henry Chen)
('ACC0008', 'TLT',   'BUY',  50.0000,  95.40, '2026-05-01', 0, NOW(), NOW(), 'SYSTEM'),
('ACC0008', 'AGG',   'BUY',  25.0000,  85.60, '2026-05-02', 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0009 (Isla Brown)
('ACC0009', 'DIA',   'BUY',   2.0000, 385.90, '2026-05-09', 0, NOW(), NOW(), 'SYSTEM'),
('ACC0009', 'GOOGL', 'BUY',   3.0000, 158.75, '2026-05-10', 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0010 (Jack Turner)
('ACC0010', 'LQD',   'BUY',  15.0000,  98.20, '2026-05-16', 0, NOW(), NOW(), 'SYSTEM'),
('ACC0010', 'IWM',   'BUY',   8.0000, 198.50, '2026-05-17', 0, NOW(), NOW(), 'SYSTEM');