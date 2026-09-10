#!/bin/bash

set -e  # Exit on error

echo "Starting data validation test..."

# Clean up any leftover containers aggressively
docker-compose down --remove-orphans --volumes || true
sleep 2  # Give Docker time to fully clean up
docker rm -f underfrog-postgres underfrog-app underfrog-notebooks || true
docker network prune -f || true  # Clean up orphaned networks
sleep 2  # Another pause before starting fresh

# Start services
echo "Starting Docker containers..."
docker-compose up -d

# Wait for PostgreSQL to be ready

echo "Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
    if docker exec underfrog-postgres pg_isready -U "${POSTGRES_USER}" > /dev/null 2>&1; then
        echo "✓ Database is ready"
        break
    fi
    
    if [ $i -eq 30 ]; then
        echo "✗ Database failed to start after 60 seconds"
        exit 1
    fi
    
    echo "  Attempt $i/30: Waiting for database..."
    sleep 2
done

# Validate data presence
echo "Validating tables..."
docker exec underfrog-postgres psql -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" -c "
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;
" > /tmp/db_validation.log

# Check that all required tables exist
REQUIRED_TABLES=("accounts" "instruments" "orders" "positions")
MISSING_TABLES=()

for table in "${REQUIRED_TABLES[@]}"; do
    if grep -q "$table" /tmp/db_validation.log; then
        echo "✓ Table '$table' exists"
    else
        echo "✗ Table '$table' missing"
        MISSING_TABLES+=("$table")
    fi
done

# Report results
if [ ${#MISSING_TABLES[@]} -eq 0 ]; then
    echo "✓ All required tables exist"
    exit 0
else
    echo "✗ Missing tables: ${MISSING_TABLES[*]}"
    echo "Database tables:"
    cat /tmp/db_validation.log
    exit 1
fi
