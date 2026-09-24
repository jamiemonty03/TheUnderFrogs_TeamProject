-- Initial set of tickers for the ETL to fetch from yfinance

INSERT INTO tracked_tickers (symbol) VALUES
    -- Stocks
    ('AAPL'), ('MSFT'), ('JPM'), ('TSLA'), ('GOOGL'),
    ('AMZN'), ('NVDA'), ('XOM'), ('JNJ'), ('KO'),
    -- ETFs
    ('SPY'), ('QQQ'), ('VTI'), ('IWM'), ('DIA'),
    ('EFA'), ('EEM'), ('XLF'), ('XLK'), ('XLE'),
    -- Bonds
    ('AGG'), ('TLT'), ('LQD'), ('BND'), ('SHY'),
    ('IEF'), ('HYG'), ('MUB'), ('TIP'), ('BNDX');
