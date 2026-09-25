-- Dummy data for orders

INSERT INTO orders (order_id, idempotency_key, account_id, symbol, side, quantity, price, order_status, version, created_at, last_updated, updated_by) VALUES
-- ACC0001 (Alice Johnson)
('a1b2c3d4-e5f6-47a8-b9c0-d1e2f3a4b5c6', '9f8e7d6c-5b4a-3928-1706-f5e4d3c2b1a0', 'ACC0001', 'AAPL',  'BUY',  10, 190.25, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
('b3c4d5e6-f7a8-49b0-c1d2-e3f4a5b6c7d8', '8e7d6c5b-4a39-3827-1605-e4d3c2b1a09f', 'ACC0001', 'SPY',   'BUY',   5, 445.00, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
('c5d6e7f8-a9b0-41c2-d3e4-f5a6b7c8d9ea', '7d6c5b4a-3928-3716-0514-d3c2b1a09f8e', 'ACC0001', 'BND',   'SELL',  3,  80.50, 'NEW',       0, NOW(), NOW(), 'SYSTEM'),
-- ACC0002 (Bob Smith)
('c7d8e9f0-a1b2-43c4-d5e6-f7a8b9c0d1e2', '1e2d3c4b-5a69-3847-2635-a4b3c2d1e0f9', 'ACC0002', 'MSFT',  'BUY',   5, 410.10, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
('d9eaf1a2-b3c4-45d6-e7f8-a9b0c1d2e3f4', '2d3c4b5a-6978-3956-2744-b5a4d3c2f1e0', 'ACC0002', 'JPM',   'BUY',   8, 156.75, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
('ebe1f2a3-c4d5-47e7-f8a9-b0c1d2e3f4a5', '3c4b5a69-7889-4067-3855-c6b5e4d3a2f1', 'ACC0002', 'QQQ',   'BUY',   2, 385.20, 'REJECTED',  0, NOW(), NOW(), 'SYSTEM'),
-- ACC0003 (Carla Diaz)
('e3f4a5b6-c7d8-41e9-f0a1-b2c3d4e5f6a7', '3d2c1b0a-f9e8-3726-5645-b4a3c2d1e0f9', 'ACC0003', 'GOOGL', 'SELL',  8, 155.60, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
('f5a6b7c8-d9ea-43fb-a1b2-c3d4e5f6a7b8', '4e3d2c1b-0af9-3837-6756-c5b4d3e2f1a0', 'ACC0003', 'AMZN',  'BUY',   6, 182.40, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
('a7b8c9da-ebfc-45ad-b2c3-d4e5f6a7b8c9', '5f4e3d2c-1ba0-3948-7867-d6c5e4f3a2b1', 'ACC0003', 'VTI',   'BUY',  12, 245.80, 'CANCELLED', 0, NOW(), NOW(), 'SYSTEM'),
-- ACC0004 (David Lee)
('01a2b3c4-d5e6-47f8-a9b0-c1d2e3f4a5b6', '5c4b3a29-1807-3625-4534-c2b1a0f9e8d7', 'ACC0004', 'AMZN',  'BUY',   2, 178.90, 'NEW',       0, NOW(), NOW(), 'SYSTEM'),
('13243546-57a8-49b4-0abc-2d3e4f5a6b7c', '6d5c4b3a-2918-3736-5645-d3c2b1a0f9e8', 'ACC0004', 'TSLA',  'BUY',   4, 250.35, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
-- ACC0005 (Emma Wilson)
('2d3e4f5a-6b7c-41a8-b9c0-d1e2f3a4b5c6', '7a6f5e4d-3c2b-3819-0706-e5d4c3b2a1f0', 'ACC0005', 'TSLA',  'BUY',  15, 245.75, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
('3e4f5a6b-7c8d-43b9-c0d1-e2f3a4b5c6d7', '8b7a6f5e-4d3c-393a-1817-f6e5d4c3b2a1', 'ACC0005', 'NVDA',  'BUY',   7, 875.50, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
('4f5a6b7c-8d9e-45ca-d1e2-f3a4b5c6d7e8', '9c8b7a6f-5e4d-405b-2928-a7f6e5d4c3b2', 'ACC0005', 'TLT',   'SELL',  5,  95.25, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
('5a6b7c8d-9eaf-47db-e2f3-a4b5c6d7e8f9', '0adf9c8b-7a69-416c-3a39-b8a7f6e5d4c3', 'ACC0005', 'SPY',   'BUY',   3, 443.50, 'NEW',       0, NOW(), NOW(), 'SYSTEM'),
-- ACC0006 (Frank Moore)
('4f6a7b8c-9d0e-43a5-b6c7-d8e9f0a1b2c3', '9e8d7c6b-5a49-3837-2625-a4b3c2d1e0f9', 'ACC0006', 'BND',   'SELL', 20,  72.30, 'CANCELLED', 0, NOW(), NOW(), 'SYSTEM'),
('60717283-94a5-45b6-c7d8-e9f0a1b2c3d4', 'af9e8d7c-6b5a-3948-2726-b5a4c3d2e1f0', 'ACC0006', 'KO',    'BUY',  10, 61.80,  'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
-- ACC0007 (Grace Kim)
('61728394-a5b6-47c8-d9e0-f1a2b3c4d5e6', 'b2a19f8e-7d6c-3b4a-2918-07f6e5d4c3b2', 'ACC0007', 'EEM',   'BUY',   3, 125.00, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
('727384a5-b6c7-49d9-eaf1-a2b3c4d5e6f7', 'c3b2a1af-8e7d-3c5b-3a29-18f7e6d5c4b3', 'ACC0007', 'EFA',   'BUY',  20, 78.40,  'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
('8384a5b6-c7d8-41ea-fbac-b3c4d5e6f7a8', 'd4c3b2a0-9f8e-3d6c-4b3a-29a8f7e6d5c4', 'ACC0007', 'XOM',   'SELL',  5, 105.25, 'NEW',       0, NOW(), NOW(), 'SYSTEM'),
-- ACC0008 (Henry Chen)
('835647a6-b8c9-41da-ebe0-f2a3b4c5d6e7', 'd3c2b1a0-9f8e-3d7c-6b5a-49382716f5e4', 'ACC0008', 'TLT',   'BUY',  50,  95.40, 'NEW',       0, NOW(), NOW(), 'SYSTEM'),
('94685ab7-c9da-43eb-fccf-a3b4c5d6e7f8', 'e4d3c2b1-a0af-3e8d-7c6b-5a49382817f6', 'ACC0008', 'AGG',   'BUY',  25,  85.60, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
-- ACC0009 (Isla Brown)
('a5b6c7d8-e9f0-43a1-b2c3-d4e5f6a7b8c9', 'f4e3d2c1-b0a9-3f8e-7d6c-5b4a39281706', 'ACC0009', 'IEF',   'SELL', 30,  95.50, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
('b6c7d8e9-faa1-45b2-c3d4-e5f6a7b8c9da', '05f4e3d2-c1b0-40a9-8e7d-6c5b4a392817', 'ACC0009', 'DIA',   'BUY',   2, 385.90, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
('c7d8e9fa-b1c2-47c3-d4e5-f6a7b8c9daeb', '16a05f4e-3d2c-41b1-9f8e-7d6c5b4a3928', 'ACC0009', 'GOOGL', 'BUY',   3, 158.75, 'NEW',       0, NOW(), NOW(), 'SYSTEM'),
-- ACC0010 (Jack Turner)
('c7d8e9fa-b1c2-41d3-e4f5-a6b7c8d9eaf0', '0f9e8d7c-6b5a-3948-2737-16a5f4e3d2c1', 'ACC0010', 'JNJ',   'BUY',  50, 158.40, 'REJECTED',  0, NOW(), NOW(), 'SYSTEM'),
('d8e9fabb-c2d3-43e4-f5a6-b7c8d9eaf0b1', '20b1a0af-7c6b-42c2-a09f-8e7d6c5b4a39', 'ACC0010', 'LQD',   'BUY',  15,  98.20, 'FILLED',    0, NOW(), NOW(), 'SYSTEM'),
('e9fabbcc-d3e4-45f5-a6b7-c8d9eaf0b1c2', '31c2b1a0-8d7c-43d3-b1a0-9f8e7d6c5b4a', 'ACC0010', 'IWM',   'BUY',   8, 198.50, 'FILLED',    0, NOW(), NOW(), 'SYSTEM');
