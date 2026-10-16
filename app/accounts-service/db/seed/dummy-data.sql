-- user_id is the user's id in auth-db (see app/auth-service/db/seed/01-seed-users.sql)
INSERT INTO accounts (account_id, user_id, holder_name, cash_balance, status, version, created_at, last_updated, updated_by)
SELECT seed.account_id, seed.user_id, seed.holder_name, seed.cash_balance, seed.status, 0, NOW(), NOW(), 'SYSTEM'
FROM (VALUES
    ('ACC0001', 1 , 'alice',  'Alice Johnson', 10500.75::NUMERIC(18,2), 'ACTIVE'),
    ('ACC0002', 2 , 'bob',    'Bob Smith',      2300.00::NUMERIC(18,2), 'ACTIVE'),
    ('ACC0003', 3 , 'carla',  'Carla Diaz',    54000.20::NUMERIC(18,2), 'ACTIVE'),
    ('ACC0004', 4 , 'david',  'David Lee',       150.50::NUMERIC(18,2), 'SUSPENDED'),
    ('ACC0005', 5 , 'emma',   'Emma Wilson',   98000.00::NUMERIC(18,2), 'ACTIVE'),
    ('ACC0006', 6 , 'frank',  'Frank Moore',    4200.10::NUMERIC(18,2), 'CLOSED'),
    ('ACC0007', 7 , 'grace',  'Grace Kim',     12750.30::NUMERIC(18,2), 'ACTIVE'),
    ('ACC0008', 8 , 'henry',  'Henry Chen',       800.00::NUMERIC(18,2), 'ACTIVE'),
    ('ACC0009', 9 , 'isla',   'Isla Brown',    33000.00::NUMERIC(18,2), 'ACTIVE'),
    ('ACC0010', 10, 'jack',   'Jack Turner',    6100.45::NUMERIC(18,2), 'SUSPENDED'),
    ('ACC0011', 11, 'demo',   'Demo Trader',  100000.00::NUMERIC(18,2), 'ACTIVE')
) AS seed(account_id, user_id, username, holder_name, cash_balance, status)
ON CONFLICT (account_id) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    holder_name = EXCLUDED.holder_name,
    cash_balance = EXCLUDED.cash_balance,
    status = EXCLUDED.status,
    last_updated = NOW(),
    updated_by = 'SYSTEM';
