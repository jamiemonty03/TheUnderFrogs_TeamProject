DROP TABLE IF EXISTS raw_etfs CASCADE;

CREATE TABLE raw_etfs (
    symbol                  VARCHAR(10) PRIMARY KEY REFERENCES instruments(symbol),
    category                VARCHAR(100),
    fund_family             VARCHAR(100),
    legal_type              VARCHAR(100),
    net_expense_ratio       NUMERIC(10, 6),
    nav_price               NUMERIC(18, 4),
    total_assets            NUMERIC(20, 2),
    net_assets              NUMERIC(20, 2),
    ytd_return              NUMERIC(10, 6),
    three_year_avg_return   NUMERIC(10, 6),
    five_year_avg_return    NUMERIC(10, 6),
    beta_3_year             NUMERIC(10, 4),
    distribution_yield      NUMERIC(10, 6),
    version       INTEGER NOT NULL DEFAULT 0,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated  TIMESTAMP  NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);