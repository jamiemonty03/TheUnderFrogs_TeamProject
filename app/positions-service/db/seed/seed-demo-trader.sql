-- Demo Trader (ACC0011) history for the demo video. Runs after dummy-data.sql; safe to re-run.
INSERT INTO portfolios (account_id, name, risk, benchmark, is_default) VALUES
('ACC0011', 'Growth', 'HIGH', 'NASDAQ-100', FALSE),
('ACC0011', 'Income', 'LOW', 'Bloomberg US Aggregate', FALSE)
ON CONFLICT (account_id, name) DO NOTHING;

UPDATE portfolios SET benchmark = 'S&P 500' WHERE account_id = 'ACC0011' AND is_default;

DELETE FROM position_movements WHERE account_id = 'ACC0011';
DELETE FROM positions WHERE account_id = 'ACC0011';

INSERT INTO positions (account_id, portfolio_id, symbol, quantity, average_cost, version, created_at, last_updated, updated_by)
SELECT 'ACC0011', p.portfolio_id, h.symbol, h.quantity, h.average_cost, 0, NOW(), NOW(), 'demo'
FROM (VALUES
    ('Growth', 'AAPL', 25.0000, 265.7000),
    ('Growth', 'NVDA', 40.0000, 199.4200),
    ('Main', 'SPY', 20.0000, 700.5300),
    ('Growth', 'MSFT', 15.0000, 422.4500),
    ('Main', 'GOOGL', 12.0000, 331.8800),
    ('Growth', 'TSLA', 10.0000, 386.4200),
    ('Income', 'BND', 100.0000, 72.2800),
    ('Main', 'JPM', 10.0000, 343.5200),
    ('Main', 'AMZN', 15.0000, 247.5500),
    ('Income', 'KO', 50.0000, 81.4800),
    ('Income', 'AGG', 60.0000, 96.7200)
) AS h(portfolio, symbol, quantity, average_cost)
JOIN portfolios p ON p.account_id = 'ACC0011' AND (p.name = h.portfolio OR (h.portfolio = 'Main' AND p.is_default));

INSERT INTO position_movements (order_id, movement_type, account_id, portfolio_id, symbol, quantity, price, created_at)
SELECT m.order_id, m.movement_type, 'ACC0011', p.portfolio_id, m.symbol, m.quantity, m.price, m.created_at::timestamp
FROM (VALUES
    ('030d1548-dc09-5090-8747-a0ec8c6c262b', 'BUY', 'Growth', 'AAPL', 35.0000, 265.70, '2026-04-20 14:35:10'),
    ('5b94e84f-4ae8-551f-8634-f543f67ab65b', 'BUY', 'Growth', 'NVDA', 50.0000, 199.42, '2026-04-20 14:41:52'),
    ('c24bfea5-ee51-5736-927f-26aa14fa0475', 'BUY', 'Main', 'SPY', 20.0000, 700.53, '2026-04-20 15:02:07'),
    ('ff3bc383-f896-5f44-bc7b-b1a6d58c832b', 'BUY', 'Growth', 'MSFT', 15.0000, 422.45, '2026-04-21 13:48:33'),
    ('b993bf3d-bf0c-568d-aa96-5fac1727e86c', 'BUY', 'Main', 'GOOGL', 12.0000, 331.88, '2026-04-21 14:20:15'),
    ('dfc0b68c-aa3d-554a-9473-0fbbb3c1b338', 'BUY', 'Growth', 'TSLA', 10.0000, 386.42, '2026-04-22 15:11:40'),
    ('32dea018-c918-5d19-88a4-26ff15e0a530', 'BUY', 'Income', 'BND', 100.0000, 72.28, '2026-04-22 15:30:02'),
    ('737197e6-d701-5230-8b88-dc4d5a9d991d', 'BUY', 'Main', 'JPM', 10.0000, 343.52, '2026-07-20 14:05:26'),
    ('e3b5c8c5-3259-5c48-8248-54c41ce3c9ef', 'BUY', 'Main', 'AMZN', 15.0000, 247.55, '2026-07-20 14:09:58'),
    ('401c1df1-1304-513c-8287-964fafe4ecee', 'BUY', 'Income', 'KO', 50.0000, 81.48, '2026-07-21 13:55:14'),
    ('ddeafd53-3725-5125-8d08-f33eedae2e55', 'BUY', 'Income', 'AGG', 60.0000, 96.72, '2026-07-21 14:02:47'),
    ('582dab27-5ee8-5273-a7b1-4443adb25cfd', 'SELL', 'Growth', 'AAPL', 10.0000, 336.13, '2026-09-18 15:22:31'),
    ('76f54640-db5f-5efa-a1cb-bc31eeb3e343', 'SELL', 'Growth', 'NVDA', 10.0000, 222.27, '2026-09-18 15:26:09')
) AS m(order_id, movement_type, portfolio, symbol, quantity, price, created_at)
JOIN portfolios p ON p.account_id = 'ACC0011' AND (p.name = m.portfolio OR (m.portfolio = 'Main' AND p.is_default));
