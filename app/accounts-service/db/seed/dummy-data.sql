-- Dummy data for accounts

INSERT INTO accounts (account_id, holder_name, cash_balance, status, version, created_at, last_updated, updated_by) VALUES
('ACC0001', 'Alice Johnson',  10500.75, 'ACTIVE',   0, NOW(), NOW(), 'SYSTEM'),
('ACC0002', 'Bob Smith',       2300.00, 'ACTIVE',   0, NOW(), NOW(), 'SYSTEM'),
('ACC0003', 'Carla Diaz',     54000.20, 'ACTIVE',   0, NOW(), NOW(), 'SYSTEM'),
('ACC0004', 'David Lee',        150.50, 'SUSPENDED',0, NOW(), NOW(), 'SYSTEM'),
('ACC0005', 'Emma Wilson',    98000.00, 'ACTIVE',   0, NOW(), NOW(), 'SYSTEM'),
('ACC0006', 'Frank Moore',     4200.10, 'INACTIVE',   0, NOW(), NOW(), 'SYSTEM'),
('ACC0007', 'Grace Kim',      12750.30, 'ACTIVE',   0, NOW(), NOW(), 'SYSTEM'),
('ACC0008', 'Henry Chen',       800.00, 'ACTIVE',   0, NOW(), NOW(), 'SYSTEM'),
('ACC0009', 'Isla Brown',     33000.00, 'ACTIVE',   0, NOW(), NOW(), 'SYSTEM'),
('ACC0010', 'Jack Turner',     6100.45, 'SUSPENDED',0, NOW(), NOW(), 'SYSTEM');
