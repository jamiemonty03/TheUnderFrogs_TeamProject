DROP TABLE IF EXISTS price_metrics;

CREATE TABLE price_metrics (
    id SERIAL PRIMARY KEY,
    ticker VARCHAR(100) NOT NULL,
    trade_date DATE NOT NULL,
    close_price NUMERIC(18, 4) NOT NULL,
    daily_return NUMERIC(18, 8),
    moving_avg_20 NUMERIC(18, 4),
    moving_avg_50 NUMERIC(18, 4),
    avg_volume_30d NUMERIC(20, 2),
    volatility_30d NUMERIC(18, 8),
    volume_spike_ratio NUMERIC(18, 4),
    momentum_score NUMERIC(6, 2),
    created_at TIMESTAMP NOT NULL,
    UNIQUE(ticker, trade_date)
);

CREATE INDEX idx_price_metrics_ticker_date 
    ON price_metrics(ticker, trade_date);

CREATE INDEX idx_price_metrics_created_at 
    ON price_metrics(created_at);
