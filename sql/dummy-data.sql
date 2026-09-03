-- Dummy data for accounts, instruments, orders, and positions tables

INSERT INTO accounts (account_id, holder_name, cash_balance, status, version, last_updated) VALUES
('ACC0001', 'Alice Johnson',  10500.75, 'ACTIVE',   0, NOW()),
('ACC0002', 'Bob Smith',       2300.00, 'ACTIVE',   0, NOW()),
('ACC0003', 'Carla Diaz',     54000.20, 'ACTIVE',   0, NOW()),
('ACC0004', 'David Lee',        150.50, 'SUSPENDED',0, NOW()),
('ACC0005', 'Emma Wilson',    98000.00, 'ACTIVE',   0, NOW()),
('ACC0006', 'Frank Moore',     4200.10, 'CLOSED',   0, NOW()),
('ACC0007', 'Grace Kim',      12750.30, 'ACTIVE',   0, NOW()),
('ACC0008', 'Henry Chen',       800.00, 'ACTIVE',   0, NOW()),
('ACC0009', 'Isla Brown',     33000.00, 'ACTIVE',   0, NOW()),
('ACC0010', 'Jack Turner',     6100.45, 'SUSPENDED',0, NOW());

INSERT INTO instruments (symbol, name, asset_class, currency, tradable) VALUES
('AAPL', 'Apple Inc.',                     'Equity',    'USD', TRUE),
('MSFT', 'Microsoft Corp.',                'Equity',    'USD', TRUE),
('GOOGL','Alphabet Inc.',                  'Equity',    'USD', TRUE),
('AMZN', 'Amazon.com Inc.',                'Equity',    'USD', TRUE),
('TSLA', 'Tesla Inc.',                     'Equity',    'USD', TRUE),
('BND',  'Vanguard Total Bond Market ETF', 'Bond',      'USD', TRUE),
('GLD',  'SPDR Gold Shares',               'Commodity', 'USD', TRUE),
('TLT',  'iShares 20+ Year Treasury Bond ETF', 'Bond',  'USD', TRUE),
('SLV',  'iShares Silver Trust',           'Commodity', 'USD', TRUE),
('VOD',  'Vodafone Group',                 'Equity',    'GBP', FALSE);

INSERT INTO orders (order_id, account_id, symbol, side, quantity, price, order_status, created_at) VALUES
('11111111-1111-1111-1111-111111111111', 'ACC0001', 'AAPL',  'BUY',  10, 190.25, 'FILLED',    NOW()),
('22222222-2222-2222-2222-222222222222', 'ACC0002', 'MSFT',  'BUY',   5, 410.10, 'FILLED',    NOW()),
('33333333-3333-3333-3333-333333333333', 'ACC0003', 'GOOGL', 'SELL',  8, 155.60, 'FILLED',    NOW()),
('44444444-4444-4444-4444-444444444444', 'ACC0004', 'AMZN',  'BUY',   2, 178.90, 'NEW',       NOW()),
('55555555-5555-5555-5555-555555555555', 'ACC0005', 'TSLA',  'BUY',  15, 245.75, 'FILLED',    NOW()),
('66666666-6666-6666-6666-666666666666', 'ACC0006', 'BND',   'SELL', 20,  72.30, 'CANCELLED', NOW()),
('77777777-7777-7777-7777-777777777777', 'ACC0007', 'GLD',   'BUY',   3, 210.00, 'FILLED',    NOW()),
('88888888-8888-8888-8888-888888888888', 'ACC0008', 'TLT',   'BUY',  50,  95.40, 'NEW',       NOW()),
('99999999-9999-9999-9999-999999999999', 'ACC0009', 'SLV',   'SELL', 30,  22.50, 'FILLED',    NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ACC0010', 'VOD',   'BUY',  50,   0.85, 'REJECTED',  NOW());

INSERT INTO positions (account_id, symbol, quantity, average_cost) VALUES
('ACC0001', 'AAPL',  10.0000, 190.25),
('ACC0002', 'MSFT',   5.0000, 410.10),
('ACC0003', 'GOOGL',  8.0000, 155.60),
('ACC0004', 'AMZN',   2.0000, 178.90),
('ACC0005', 'TSLA',  15.0000, 245.75),
('ACC0006', 'BND',   20.0000,  72.30),
('ACC0007', 'GLD',    3.0000, 210.00),
('ACC0008', 'TLT',   50.0000,  95.40),
('ACC0009', 'SLV',   30.0000,  22.50),
('ACC0010', 'VOD',   50.0000,   0.85);