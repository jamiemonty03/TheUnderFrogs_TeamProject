INSERT INTO auth (user_id, failed_login_attempts, created_at, updated_at)
SELECT id, 0, NOW(), NOW() FROM users ORDER BY id;
