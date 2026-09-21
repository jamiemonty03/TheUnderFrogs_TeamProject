DROP TABLE IF EXISTS instruments_metrics;

CREATE TABLE instruments_metrics (
    symbol VARCHAR(10) NOT NULL PRIMARY KEY REFERENCES instruments(symbol),
    latest_price NUMERIC(18, 4),         
    latest_date DATE,                    
    price_52w_high NUMERIC(18, 4),        
    price_52w_low NUMERIC(18, 4),         
    ytd_return NUMERIC(8, 4),            
    one_year_return NUMERIC(8, 4),        
    max_drawdown NUMERIC(8, 4),           
    last_price_update DATE,               
    days_since_update INTEGER,            
    asset_class VARCHAR(50) NOT NULL,    
    currency CHAR(3) NOT NULL,           
    exchange VARCHAR(50),               
    version INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by VARCHAR(255) NOT NULL DEFAULT 'SYSTEM',
    
    CONSTRAINT valid_returns CHECK (ytd_return IS NULL OR (ytd_return >= -100 AND ytd_return <= 10000)),
    CONSTRAINT valid_drawdown CHECK (max_drawdown IS NULL OR (max_drawdown >= -100 AND max_drawdown <= 0))
);

CREATE INDEX idx_instruments_metrics_asset_class ON instruments_metrics(asset_class);
CREATE INDEX idx_instruments_metrics_currency ON instruments_metrics(currency);
CREATE INDEX idx_instruments_metrics_ytd_return ON instruments_metrics(ytd_return DESC);
CREATE INDEX idx_instruments_metrics_max_drawdown ON instruments_metrics(max_drawdown DESC);
CREATE INDEX idx_instruments_metrics_days_since_update ON instruments_metrics(days_since_update DESC);

