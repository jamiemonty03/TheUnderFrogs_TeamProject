#!/usr/bin/env bash

set -euo pipefail

echo "Checking existing database containers and required tables..."

# Each target is: service name | container name | required tables.
DB_TARGETS=(
    "accounts|accounts-db|accounts"
    "instruments|instruments-db|instruments"
    "orders|orders-db|orders client_trades"
    "positions|positions-db|positions"
)

for target in "${DB_TARGETS[@]}"; do
    IFS='|' read -r service container required_tables <<< "$target"
    database="${service}_db"

    if ! docker inspect "$container" >/dev/null 2>&1; then
        echo "✗ Required container '$container' does not exist. Start the database stack before validation." >&2
        exit 1
    fi

    running=$(docker inspect --format '{{.State.Running}}' "$container")
    if [[ "$running" != "true" ]]; then
        echo "✗ Database container '$container' is not running." >&2
        exit 1
    fi

    if ! docker exec "$container" sh -c \
        'pg_isready -U "$SPRING_DATASOURCE_USERNAME" -d "${SPRING_DATASOURCE_URL##*/}"' \
        >/dev/null 2>&1; then
        echo "✗ Database '$database' in '$container' is not accepting connections." >&2
        exit 1
    fi
    echo "✓ $container is running and accepting connections"

    existing_tables=$(docker exec "$container" sh -c \
        'psql -U "$SPRING_DATASOURCE_USERNAME" -d "${SPRING_DATASOURCE_URL##*/}" -Atc "SELECT table_name FROM information_schema.tables WHERE table_schema = '\''public'\'' ORDER BY table_name"')

    for table in $required_tables; do
        if grep -Fxq "$table" <<< "$existing_tables"; then
            echo "✓ Table '$table' exists in $database"
        else
            echo "✗ Table '$table' is missing from $database" >&2
            exit 1
        fi
    done
done

echo "✓ All existing databases are running and contain the required tables."
