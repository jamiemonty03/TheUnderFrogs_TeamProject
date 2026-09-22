#!/bin/bash

set -e

echo "========================================="
echo "TheUnderFrogs ETL Pipeline Setup"
echo "========================================="

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Error handling function
error_exit() {
    echo -e "${RED}✗ Error: $1${NC}" >&2
    exit 1
}

# Run psql inside a service's database container using that service's .env credentials
# (SPRING_DATASOURCE_*). Usage: db_psql <container> [-i] [psql args...]
db_psql() {
    local container="$1"; shift
    local flags=""
    if [ "$1" = "-i" ]; then flags="-i"; shift; fi
    docker exec $flags "$container" sh -c 'psql -U "$SPRING_DATASOURCE_USERNAME" -d "${SPRING_DATASOURCE_URL##*/}" "$@"' _ "$@"
}

# Check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"
    
    command -v docker >/dev/null 2>&1 || error_exit "Docker not installed"
    echo -e "${GREEN}✓ Docker found${NC}"
    
    docker ps >/dev/null 2>&1 || error_exit "Docker daemon not running"
    echo -e "${GREEN}✓ Docker daemon running${NC}"
    
    if [ "$MODE" = "full" ]; then
        command -v mvn >/dev/null 2>&1 || error_exit "Maven not installed"
        echo -e "${GREEN}✓ Maven found${NC}"
    fi
    
    [ -f "docker-compose.yml" ] || error_exit "docker-compose.yml not found in current directory"
    for svc in accounts instruments orders positions; do
        [ -f "app/$svc-service/.env" ] || error_exit "app/$svc-service/.env missing (copy it from .env.example and set the password)"
    done
    echo -e "${GREEN}✓ docker-compose.yml found${NC}\n"
}

if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
    echo "Usage: ./db/scripts/setup-pipeline.sh [option]"
    echo ""
    echo "Options:"
    echo "  full        Build jars, create tables, and populate (default)"
    echo "  skip-build  Skip Maven build, just create tables and populate"
    echo "  populate    Only run population scripts (no table creation)"
    exit 0
fi

MODE="${1:-full}"

if [ "$MODE" != "full" ] && [ "$MODE" != "skip-build" ] && [ "$MODE" != "populate" ]; then
    error_exit "Invalid option: $MODE (use --help for usage)"
fi

# Run prerequisite checks
check_prerequisites

if [ "$MODE" = "full" ] || [ "$MODE" = "skip-build" ]; then
    if [ "$MODE" = "full" ]; then
        echo -e "${YELLOW}Building Java services...${NC}"
        for svc in accounts instruments orders positions; do
            mvn -q -B -f app/$svc-service/pom.xml clean package -Dmaven.test.skip=true || error_exit "Maven build failed for $svc-service"
        done
        echo -e "${GREEN}✓ Build complete${NC}\n"
    fi

    echo -e "${YELLOW}Starting Docker containers...${NC}"
    docker-compose down --remove-orphans 2>/dev/null || true
    docker-compose up -d || error_exit "Failed to start Docker containers"
    echo -e "${GREEN}✓ Containers started${NC}\n"

    echo -e "${YELLOW}Waiting for databases...${NC}"
    for container in accounts-db instruments-db orders-db positions-db; do
        DB_READY=false
        for i in {1..30}; do
            if docker exec "$container" sh -c 'pg_isready -U "$SPRING_DATASOURCE_USERNAME"' > /dev/null 2>&1; then
                echo -e "${GREEN}✓ $container ready${NC}"
                DB_READY=true
                break
            fi
            sleep 2
        done

        [ "$DB_READY" = true ] || error_exit "$container failed to start after 60 seconds"
    done
    echo ""

    echo -e "${YELLOW}Creating tables...${NC}"
    
    # Each service owns its own database container; run that service's schema files
    for container in accounts-db instruments-db orders-db positions-db; do
        for sql_file in $(docker exec "$container" ls /docker-entrypoint-initdb.d | sort); do
            db_psql "$container" -f /docker-entrypoint-initdb.d/"$sql_file" || error_exit "Failed to create table from $container:$sql_file"
        done
    done
    
    echo -e "${GREEN}✓ Tables created${NC}\n"
else
    echo -e "${YELLOW}Skipping Docker startup (populate mode)${NC}\n"
fi

echo -e "${YELLOW}Populating tables...${NC}"

# Array of Python scripts to execute
PYTHON_SCRIPTS=(
    "01-generate_raw_instruments.py|Instruments"
    "02-generate_raw_prices.py|Prices"
    "03-etl_prices.py|ETL Prices"
    "04-etl_stocks.py|ETL Stocks"
    "05-etl_etfs.py|ETL ETFs"
    "06-etl_bonds.py|ETL Bonds"
    "etl_instruments_metrics.py|ETL Instruments Metrics"
    "etl_price_metrics.py|ETL Price Metrics"
)

for script_info in "${PYTHON_SCRIPTS[@]}"; do
    IFS='|' read -r script_path label <<< "$script_info"
    docker exec underfrog-python python /db/etl/"$script_path" || error_exit "Failed to run $label ($script_path)"
    echo -e "${GREEN}✓ $label${NC}"
done

echo ""
echo -e "${YELLOW}Seeding dummy data...${NC}"

seed_db() {
    local container="$1" table="$2" seed_file="$3"
    [ -f "$seed_file" ] || error_exit "$seed_file not found"
    local count
    count=$(db_psql "$container" -Atc "SELECT COUNT(*) FROM $table;") || error_exit "Failed to check existing $table"
    if [ "$count" -gt 0 ]; then
        echo -e "${YELLOW}⚠ $table already has $count rows, skipping seed${NC}"
    else
        db_psql "$container" -i -v ON_ERROR_STOP=1 --single-transaction < "$seed_file" || error_exit "Failed to load $seed_file"
        echo -e "${GREEN}✓ $table seeded${NC}"
    fi
}

seed_db accounts-db  accounts  app/accounts-service/db/seed/dummy-data.sql
seed_db orders-db    orders    app/orders-service/db/seed/dummy-data.sql
seed_db orders-db    client_trades app/orders-service/db/seed/client-trades.sql
seed_db positions-db positions app/positions-service/db/seed/dummy-data.sql

echo ""
echo -e "${YELLOW}Checking historical trades against current positions...${NC}"

trades_file=$(mktemp)
positions_file=$(mktemp)

db_psql orders-db -Atc "
    SELECT account_id || '|' || symbol || '|' || net_quantity
    FROM (
        SELECT account_id, symbol,
               SUM(CASE WHEN trade_type = 'BUY' THEN quantity ELSE -quantity END)::NUMERIC(18,4) AS net_quantity
        FROM client_trades
        GROUP BY account_id, symbol
    ) trade_totals
    ORDER BY account_id, symbol;
" > "$trades_file" || error_exit "Failed to calculate client_trades totals"

db_psql positions-db -Atc "
    SELECT account_id || '|' || symbol || '|' || quantity::NUMERIC(18,4)
    FROM positions
    ORDER BY account_id, symbol;
" > "$positions_file" || error_exit "Failed to read positions totals"

if diff -u "$positions_file" "$trades_file"; then
    echo -e "${GREEN}✓ Historical trades reconcile with positions${NC}"
else
    error_exit "client_trades net quantities do not match positions"
fi

rm -f "$trades_file" "$positions_file"

echo ""
echo -e "${GREEN}========================================="
echo "✓ Setup completed successfully!"
echo "=========================================${NC}"