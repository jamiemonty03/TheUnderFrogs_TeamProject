-- Seed users, moved here from accounts-db when auth-service took over users (S8-7).
-- Inserted in this order so their ids (1-11) match accounts.user_id in accounts-db.
-- Passwords are the original BCrypt hashes; auth-service re-hashes them to argon2id on first login.
INSERT INTO users (username, email, password_hash, full_name, roles, account_id, is_active) VALUES
('alice', 'alice.johnson@example.com', '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Alice Johnson', ARRAY['TRADER'], 'ACC0001', TRUE),
('bob', 'bob.smith@example.com', '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Bob Smith', ARRAY['TRADER'], 'ACC0002', TRUE),
('carla', 'carla.diaz@example.com', '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Carla Diaz', ARRAY['TRADER'], 'ACC0003', TRUE),
('david', 'david.lee@example.com', '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'David Lee', ARRAY['TRADER'], 'ACC0004', TRUE),
('emma', 'emma.wilson@example.com', '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Emma Wilson', ARRAY['TRADER'], 'ACC0005', TRUE),
('frank', 'frank.moore@example.com', '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Frank Moore', ARRAY['TRADER'], 'ACC0006', TRUE),
('grace', 'grace.kim@example.com', '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Grace Kim', ARRAY['TRADER'], 'ACC0007', TRUE),
('henry', 'henry.chen@example.com', '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Henry Chen', ARRAY['TRADER'], 'ACC0008', TRUE),
('isla', 'isla.brown@example.com', '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Isla Brown', ARRAY['TRADER'], 'ACC0009', TRUE),
('jack', 'jack.turner@example.com', '$2a$10$vUWfBQn8LxSy/meoJXppD.QSv4VmWbGMFiR5Do9.pBy0XLLTcLhKG', 'Jack Turner', ARRAY['TRADER'], 'ACC0010', TRUE),
('demo', 'demo.trader@example.com', '$2a$10$tEN44im3u450Nu8rjYmV2./rNJUmlmQsl8jIAmoJjbOWRMXILuxfW', 'Demo Trader', ARRAY['TRADER'], 'ACC0011', TRUE);
