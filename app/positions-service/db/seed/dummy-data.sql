-- Dummy data for positions

INSERT INTO positions (account_id, symbol, quantity, average_cost, version, created_at, last_updated, updated_by) VALUES
-- ACC0001 (Alice Johnson)
('ACC0001', 'AAPL',  10.0000, 190.25, 0, NOW(), NOW(), 'SYSTEM'),
('ACC0001', 'SPY',    5.0000, 445.00, 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0002 (Bob Smith)
('ACC0002', 'MSFT',   5.0000, 410.10, 0, NOW(), NOW(), 'SYSTEM'),
('ACC0002', 'JPM',    8.0000, 156.75, 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0003 (Carla Diaz)
('ACC0003', 'AMZN',   6.0000, 182.40, 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0004 (David Lee)
('ACC0004', 'TSLA',   4.0000, 250.35, 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0005 (Emma Wilson)
('ACC0005', 'TSLA',  15.0000, 245.75, 0, NOW(), NOW(), 'SYSTEM'),
('ACC0005', 'NVDA',   7.0000, 875.50, 0, NOW(), NOW(), 'SYSTEM'),
('ACC0005', 'SPY',    3.0000, 443.50, 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0006 (Frank Moore)
('ACC0006', 'KO',    10.0000,  61.80, 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0007 (Grace Kim)
('ACC0007', 'EFA',   20.0000,  78.40, 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0008 (Henry Chen)
('ACC0008', 'TLT',   50.0000,  95.40, 0, NOW(), NOW(), 'SYSTEM'),
('ACC0008', 'AGG',   25.0000,  85.60, 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0009 (Isla Brown)
('ACC0009', 'DIA',    2.0000, 385.90, 0, NOW(), NOW(), 'SYSTEM'),
('ACC0009', 'GOOGL',  3.0000, 158.75, 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0010 (Jack Turner)
('ACC0010', 'LQD',   15.0000,  98.20, 0, NOW(), NOW(), 'SYSTEM'),
('ACC0010', 'IWM',    8.0000, 198.50, 0, NOW(), NOW(), 'SYSTEM');
