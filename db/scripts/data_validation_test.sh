#!/bin/bash

set -e  # Exit on error

echo "Starting data validation test..."

# Each microservice owns its own database container: "service|container|required tables"
# Credentials and the database name come from app/<service>-service/.env (SPRING_DATASOURCE_*)
DB_TARGETS=(
    "accounts|accounts-db|accounts"
    "instruments|instruments-db|instruments"
    "orders|orders-db|orders client_trades"
    "positions|positions-db|positions"
)
DB_CONTAINERS="accounts-db instruments-db orders-db positions-db"

# The .env files are gitignored. In CI, create any missing ones from .env.example,
# filling in the password from POSTGRES_PASSWORD (Jenkins credential).
for target in "${DB_TARGETS[@]}"; do
    IFS='|' read -r service container tables <<< "$target"
    env_file="app/${service}-service/.env"
    if [ ! -f "$env_file" ]; then
        echo "Creating $env_file from .env.example"
        sed "s/ENTER_PASSWORD_HERE/${POSTGRES_PASSWORD:?POSTGRES_PASSWORD must be set to create $env_file}/" "app/${service}-service/.env.example" > "$env_file"
    fi
done

# Clean up any leftover containers aggressively
docker-compose down --remove-orphans --volumes || true
sleep 2  # Give Docker time to fully clean up
docker rm -f $DB_CONTAINERS underfrog-python || true
docker network prune -f || true  # Clean up orphaned networks
sleep 2  # Another pause before starting fresh

# Only the databases are needed to validate the schemas
echo "Starting database containers..."
docker-compose up -d $DB_CONTAINERS

MISSING_TABLES=()

for target in "${DB_TARGETS[@]}"; do
    IFS='|' read -r service container tables <<< "$target"

    echo "Waiting for $container to be ready..."
    for i in {1..30}; do
        if docker exec "$container" sh -c 'pg_isready -U "$SPRING_DATASOURCE_USERNAME" -d "${SPRING_DATASOURCE_URL##*/}"' > /dev/null 2>&1; then
            echo "✓ $container is ready"
            break
        fi

        if [ $i -eq 30 ]; then
            echo "✗ $container failed to start after 60 seconds"
            exit 1
        fi

        echo "  Attempt $i/30: Waiting for $container..."
        sleep 2
    done

    echo "Validating tables in $container..."
    docker exec "$container" sh -c 'psql -U "$SPRING_DATASOURCE_USERNAME" -d "${SPRING_DATASOURCE_URL##*/}" -Atc "
    SELECT table_name FROM information_schema.tables
    WHERE table_schema = '"'"'public'"'"'
    ORDER BY table_name;
    "' > /tmp/db_validation.log

    for table in $tables; do
        if grep -qx "$table" /tmp/db_validation.log; then
            echo "✓ Table '$table' exists in $container"
        else
            echo "✗ Table '$table' missing in $container"
            MISSING_TABLES+=("$container.$table")
        fi
    done
done

# Report results
if [ ${#MISSING_TABLES[@]} -eq 0 ]; then
    echo "✓ All required tables exist"
    exit 0
else
    echo "✗ Missing tables: ${MISSING_TABLES[*]}"
    exit 1
fi
