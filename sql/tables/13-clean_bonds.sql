DROP TABLE IF EXISTS clean_bonds CASCADE;

CREATE TABLE clean_bonds (
    symbol VARCHAR(10) NOT NULL,

    category VARCHAR(100),
    fund_family VARCHAR(100),
    legal_type VARCHAR(100),

    net_expense_ratio NUMERIC(10,6),

    nav_price NUMERIC(18,4) NOT NULL,

    total_assets NUMERIC(20,2) NOT NULL,

    net_assets NUMERIC(20,2),

    ytd_return NUMERIC(10,6),

    three_year_avg_return NUMERIC(10,6),

    five_year_avg_return NUMERIC(10,6),

    beta_3_year NUMERIC(10,6),

    distribution_yield NUMERIC(10,6),

     version INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    last_updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',

    PRIMARY KEY (symbol),
    FOREIGN KEY (symbol) REFERENCES instruments(symbol),

    CHECK (nav_price > 0),

    CHECK (total_assets >= 0),

    CHECK (net_assets >= 0 OR net_assets IS NULL),

    CHECK (distribution_yield >= 0 OR distribution_yield IS NULL),

    CHECK (net_expense_ratio >= 0 OR net_expense_ratio IS NULL)
);