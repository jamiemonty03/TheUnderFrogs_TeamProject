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

# Run psql inside a service's database container
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
    
    if [[ "$STAGE" == "all" || "$STAGE" == "containers" || "$STAGE" == "build" ]]; then
        command -v mvn >/dev/null 2>&1 || error_exit "Maven not installed"
        echo -e "${GREEN}✓ Maven found${NC}"
    fi
    
    [ -f "docker-compose.yml" ] || error_exit "docker-compose.yml not found in current directory"
    for svc in accounts instruments orders positions; do
        [ -f "app/$svc-service/.env" ] || error_exit "app/$svc-service/.env missing (copy it from .env.example and set the password)"
    done
    echo -e "${GREEN}✓ All prerequisites satisfied${NC}\n"
}

# Help text
if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
    echo "Usage: ./db/scripts/setup-pipeline.sh [stage]"
    echo ""
    echo "Stages:"
    echo "  all         Full setup: build → containers → tables → populate (default)"
    echo "  build       Build Maven jars only"
    echo "  containers  Spin up Docker containers only"
    echo "  tables      Create tables only (containers must be running)"
    echo "  populate    Populate data only (containers and tables must exist)"
    echo ""
    exit 0
fi

STAGE="${1:-all}"

if [[ ! "$STAGE" =~ ^(all|build|containers|tables|populate)$ ]]; then
    error_exit "Invalid stage: $STAGE (use --help for usage)"
fi

# Run prerequisite checks
check_prerequisites

# ==========================================
# STAGE 1: BUILD
# ==========================================
if [[ "$STAGE" == "all" || "$STAGE" == "build" ]]; then
    echo -e "${YELLOW}Building Java services...${NC}"
    for svc in accounts instruments orders positions; do
        mvn -q -B -f app/$svc-service/pom.xml clean package -Dmaven.test.skip=true || error_exit "Maven build failed for $svc-service"
        echo -e "${GREEN}✓ Built $svc-service${NC}"
    done
    echo ""
    [ "$STAGE" == "build" ] && exit 0
fi

# ==========================================
# STAGE 2: SPIN UP CONTAINERS
# ==========================================
if [[ "$STAGE" == "all" || "$STAGE" == "containers" ]]; then
    echo -e "${YELLOW}Starting Docker containers...${NC}"
    docker-compose down --remove-orphans 2>/dev/null || true
    docker-compose up -d || error_exit "Failed to start Docker containers"
    echo -e "${GREEN}✓ Docker compose up complete${NC}\n"

    echo -e "${YELLOW}Waiting for databases to be ready...${NC}"
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
    [ "$STAGE" == "containers" ] && exit 0
fi

# ==========================================
# STAGE 3: CREATE TABLES
# ==========================================
if [[ "$STAGE" == "all" || "$STAGE" == "tables" ]]; then
    echo -e "${YELLOW}Creating database schemas and tables...${NC}"
    
    # Array of services and their database containers
    declare -a services=("accounts" "instruments" "orders" "positions")
    
    for svc in "${services[@]}"; do
        container="${svc}-db"
        echo -e "${YELLOW}Processing $svc-service...${NC}"
        
        # Run all SQL files in order (00-schema.sql, then 01-*.sql, etc.)
        sql_files=$(docker exec "$container" sh -c "ls /docker-entrypoint-initdb.d/*.sql 2>/dev/null | sort" || echo "")
        
        if [ -z "$sql_files" ]; then
            echo -e "${YELLOW}⚠ No SQL files found for $container${NC}"
        else
            for sql_file in $sql_files; do
                filename=$(basename "$sql_file")
                db_psql "$container" -f "$sql_file" || error_exit "Failed to execute $filename in $container"
                echo -e "${GREEN}✓ Executed $filename${NC}"
            done
        fi
    done
    
    echo ""
    [ "$STAGE" == "tables" ] && exit 0
fi

# ==========================================
# STAGE 4: POPULATE DATA
# ==========================================
if [[ "$STAGE" == "all" || "$STAGE" == "populate" ]]; then
    echo -e "${YELLOW}Populating tables with data...${NC}\n"

    # Seed data function
    seed_db() {
        local container="$1" table="$2" seed_file="$3"
        [ -f "$seed_file" ] || { echo -e "${YELLOW}⚠ $seed_file not found, skipping${NC}"; return 0; }
        
        local count
        count=$(db_psql "$container" -Atc "SELECT COUNT(*) FROM $table;" 2>/dev/null || echo "0") || true
        
        if [ "$count" -gt 0 ]; then
            echo -e "${YELLOW}⚠ $table already has $count rows, skipping seed${NC}"
        else
            db_psql "$container" -i -v ON_ERROR_STOP=1 --single-transaction < "$seed_file" || error_exit "Failed to load $seed_file"
            echo -e "${GREEN}✓ $table seeded${NC}"
        fi
    }

    # The ETL reads tracked_tickers to decide what to fetch, so seed it first
    seed_db instruments-db tracked_tickers app/instruments-service/db/seed/tracked-tickers.sql
    echo ""

    # Python ETL scripts
    echo -e "${YELLOW}Running Python ETL scripts...${NC}"
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
fi

# ==========================================
# SUCCESS
# ==========================================
echo -e "${GREEN}========================================="
echo "✓ Setup stage '$STAGE' completed successfully!"
echo "=========================================${NC}"