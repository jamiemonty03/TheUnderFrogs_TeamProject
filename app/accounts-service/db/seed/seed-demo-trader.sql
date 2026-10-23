-- Demo Trader (ACC0011) history for the demo video. Runs after dummy-data.sql; safe to re-run.
DELETE FROM cash_movements WHERE account_id = 'ACC0011';

INSERT INTO cash_movements (order_id, movement_type, account_id, amount, created_at) VALUES
('stripe:pi_demo_3PqA1tFj8Ln2Xk', 'CREDIT', 'ACC0011', 100000.00, '2026-04-15 09:12:00'),
('stripe:pi_demo_3RbK9mWz4Vq7Hd', 'CREDIT', 'ACC0011', 25000.00, '2026-07-10 18:40:00'),
('030d1548-dc09-5090-8747-a0ec8c6c262b', 'DEBIT', 'ACC0011', 9299.50, '2026-04-20 14:35:10'),
('5b94e84f-4ae8-551f-8634-f543f67ab65b', 'DEBIT', 'ACC0011', 9971.00, '2026-04-20 14:41:52'),
('c24bfea5-ee51-5736-927f-26aa14fa0475', 'DEBIT', 'ACC0011', 14010.60, '2026-04-20 15:02:07'),
('ff3bc383-f896-5f44-bc7b-b1a6d58c832b', 'DEBIT', 'ACC0011', 6336.75, '2026-04-21 13:48:33'),
('b993bf3d-bf0c-568d-aa96-5fac1727e86c', 'DEBIT', 'ACC0011', 3982.56, '2026-04-21 14:20:15'),
('dfc0b68c-aa3d-554a-9473-0fbbb3c1b338', 'DEBIT', 'ACC0011', 3864.20, '2026-04-22 15:11:40'),
('32dea018-c918-5d19-88a4-26ff15e0a530', 'DEBIT', 'ACC0011', 7228.00, '2026-04-22 15:30:02'),
('737197e6-d701-5230-8b88-dc4d5a9d991d', 'DEBIT', 'ACC0011', 3435.20, '2026-07-20 14:05:26'),
('e3b5c8c5-3259-5c48-8248-54c41ce3c9ef', 'DEBIT', 'ACC0011', 3713.25, '2026-07-20 14:09:58'),
('401c1df1-1304-513c-8287-964fafe4ecee', 'DEBIT', 'ACC0011', 4074.00, '2026-07-21 13:55:14'),
('ddeafd53-3725-5125-8d08-f33eedae2e55', 'DEBIT', 'ACC0011', 5803.20, '2026-07-21 14:02:47'),
('582dab27-5ee8-5273-a7b1-4443adb25cfd', 'CREDIT', 'ACC0011', 3361.30, '2026-09-18 15:22:31'),
('76f54640-db5f-5efa-a1cb-bc31eeb3e343', 'CREDIT', 'ACC0011', 2222.70, '2026-09-18 15:26:09');

UPDATE accounts SET cash_balance = 58865.74, last_updated = NOW(), updated_by = 'SYSTEM' WHERE account_id = 'ACC0011';
