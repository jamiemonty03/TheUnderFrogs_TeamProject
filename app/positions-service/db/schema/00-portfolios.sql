DROP TABLE IF EXISTS portfolios CASCADE;
DROP SEQUENCE IF EXISTS portfolio_id_seq;

CREATE SEQUENCE portfolio_id_seq;

CREATE TABLE portfolios (
    portfolio_id  VARCHAR(32)  NOT NULL PRIMARY KEY
                  DEFAULT ('PF' || LPAD(nextval('portfolio_id_seq')::TEXT, 4, '0')),
    account_id    VARCHAR(32)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    risk          VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' CHECK (risk IN ('LOW', 'MEDIUM', 'HIGH')),
    benchmark     VARCHAR(100),
    is_default    BOOLEAN      NOT NULL DEFAULT FALSE,
    version       INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_updated  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    CONSTRAINT uq_portfolios_account_name UNIQUE (account_id, name),
    CONSTRAINT uq_portfolios_id_account UNIQUE (portfolio_id, account_id)
);

CREATE UNIQUE INDEX uq_portfolios_one_default ON portfolios(account_id) WHERE is_default;
CREATE INDEX idx_portfolios_account_id ON portfolios(account_id);
