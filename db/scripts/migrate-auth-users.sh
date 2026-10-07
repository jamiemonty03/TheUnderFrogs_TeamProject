#!/bin/bash
set -euo pipefail

EXPORT_USERS="\copy (SELECT DISTINCT ON (u.id) u.username, u.email, u.password, u.full_name, a.account_id, u.is_active, u.created_at FROM users u LEFT JOIN accounts a ON a.user_id = u.id ORDER BY u.id, a.account_id) TO STDOUT WITH (FORMAT csv)"

docker exec accounts-db sh -c 'psql -U "$SPRING_DATASOURCE_USERNAME" -d "${SPRING_DATASOURCE_URL##*/}" -v ON_ERROR_STOP=1 -c "$1"' _ "$EXPORT_USERS" \
  | docker exec -i auth-db sh -c 'psql -U "$DB_USERNAME" -d "$DB_NAME" -v ON_ERROR_STOP=1 --single-transaction -f /db/migrations/01-copy-users-from-accounts.sql'
