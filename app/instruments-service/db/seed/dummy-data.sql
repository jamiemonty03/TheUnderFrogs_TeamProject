-- Seed data for instruments

INSERT INTO instruments (symbol, name, asset_class, currency, exchange, tradable, version, created_at, last_updated, updated_by) VALUES
-- Stocks
('AAPL', 'Apple Inc.', 'Equity', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('MSFT', 'Microsoft Corporation', 'Equity', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('GOOGL', 'Alphabet Inc.', 'Equity', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('AMZN', 'Amazon.com Inc.', 'Equity', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('TSLA', 'Tesla Inc.', 'Equity', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('NVDA', 'NVIDIA Corporation', 'Equity', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('JPM', 'JPMorgan Chase & Co.', 'Equity', 'USD', 'NYSE', true, 0, NOW(), NOW(), 'SYSTEM'),
('JNJ', 'Johnson & Johnson', 'Equity', 'USD', 'NYSE', true, 0, NOW(), NOW(), 'SYSTEM'),
('KO', 'The Coca-Cola Company', 'Equity', 'USD', 'NYSE', true, 0, NOW(), NOW(), 'SYSTEM'),
('XOM', 'Exxon Mobil Corporation', 'Equity', 'USD', 'NYSE', true, 0, NOW(), NOW(), 'SYSTEM'),
-- ETFs
('SPY', 'SPDR S&P 500 ETF Trust', 'ETF', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('QQQ', 'Invesco QQQ Trust', 'ETF', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('VTI', 'Vanguard Total Stock Market ETF', 'ETF', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('EEM', 'iShares MSCI Emerging Markets ETF', 'ETF', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('EFA', 'iShares MSCI EAFE ETF', 'ETF', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('AGG', 'iShares Core U.S. Aggregate Bond ETF', 'ETF', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('DIA', 'SPDR Dow Jones Industrial Average ETF', 'ETF', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('IWM', 'iShares Russell 2000 ETF', 'ETF', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
-- Bonds
('BND', 'Vanguard Total Bond Market ETF', 'Bond', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('TLT', 'iShares 20+ Year Treasury Bond ETF', 'Bond', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('IEF', 'iShares 7-10 Year Treasury Bond ETF', 'Bond', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM'),
('LQD', 'iShares Investment Grade Corporate Bond ETF', 'Bond', 'USD', 'NASDAQ', true, 0, NOW(), NOW(), 'SYSTEM');
