
INSERT INTO users (username, email, password, full_name, is_active, version, created_at, last_updated, updated_by) VALUES
('alice',  'alice.johnson@example.com', '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Alice Johnson', TRUE, 0, NOW(), NOW(), 'SYSTEM'),
('bob',    'bob.smith@example.com',     '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Bob Smith', TRUE, 0, NOW(), NOW(), 'SYSTEM'),
('carla',  'carla.diaz@example.com',    '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Carla Diaz', TRUE, 0, NOW(), NOW(), 'SYSTEM'),
('david',  'david.lee@example.com',     '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'David Lee', TRUE, 0, NOW(), NOW(), 'SYSTEM'),
('emma',   'emma.wilson@example.com',   '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Emma Wilson', TRUE, 0, NOW(), NOW(), 'SYSTEM'),
('frank',  'frank.moore@example.com',   '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Frank Moore', TRUE, 0, NOW(), NOW(), 'SYSTEM'),
('grace',  'grace.kim@example.com',     '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Grace Kim', TRUE, 0, NOW(), NOW(), 'SYSTEM'),
('henry',  'henry.chen@example.com',    '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Henry Chen', TRUE, 0, NOW(), NOW(), 'SYSTEM'),
('isla',   'isla.brown@example.com',    '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Isla Brown', TRUE, 0, NOW(), NOW(), 'SYSTEM'),
('jack',   'jack.turner@example.com',   '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Jack Turner', TRUE, 0, NOW(), NOW(), 'SYSTEM')
ON CONFLICT (username) DO NOTHING;

INSERT INTO accounts (account_id, user_id, holder_name, cash_balance, status, version, created_at, last_updated, updated_by)
SELECT seed.account_id, users.id, seed.holder_name, seed.cash_balance, seed.status, 0, NOW(), NOW(), 'SYSTEM'
FROM (VALUES
    ('ACC0001', 'alice',  'Alice Johnson', 10500.75::NUMERIC(18,2), 'ACTIVE'),
    ('ACC0002', 'bob',    'Bob Smith',      2300.00::NUMERIC(18,2), 'ACTIVE'),
    ('ACC0003', 'carla',  'Carla Diaz',    54000.20::NUMERIC(18,2), 'ACTIVE'),
    ('ACC0004', 'david',  'David Lee',       150.50::NUMERIC(18,2), 'SUSPENDED'),
    ('ACC0005', 'emma',   'Emma Wilson',   98000.00::NUMERIC(18,2), 'ACTIVE'),
    ('ACC0006', 'frank',  'Frank Moore',    4200.10::NUMERIC(18,2), 'CLOSED'),
    ('ACC0007', 'grace',  'Grace Kim',     12750.30::NUMERIC(18,2), 'ACTIVE'),
    ('ACC0008', 'henry',  'Henry Chen',       800.00::NUMERIC(18,2), 'ACTIVE'),
    ('ACC0009', 'isla',   'Isla Brown',    33000.00::NUMERIC(18,2), 'ACTIVE'),
    ('ACC0010', 'jack',   'Jack Turner',    6100.45::NUMERIC(18,2), 'SUSPENDED')
) AS seed(account_id, username, holder_name, cash_balance, status)
JOIN users ON users.username = seed.username
ON CONFLICT (account_id) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    holder_name = EXCLUDED.holder_name,
    cash_balance = EXCLUDED.cash_balance,
    status = EXCLUDED.status,
    last_updated = NOW(),
    updated_by = 'SYSTEM';
