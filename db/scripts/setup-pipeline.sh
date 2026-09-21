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
    echo -e "${GREEN}✓ docker-compose.yml found${NC}\n"
}

if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
    echo "Usage: ./db/scripts/setup-pipeline.sh [option]"
    echo ""
    echo "Options:"
    echo "  full        Build, create tables, and populate (default)"
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
        echo -e "${YELLOW}Building Java application...${NC}"
        mvn -f app/pom.xml clean package -Dmaven.test.skip=true || error_exit "Maven build failed"
        echo -e "${GREEN}✓ Build complete${NC}\n"
    fi

    echo -e "${YELLOW}Starting Docker containers...${NC}"
    docker-compose down --remove-orphans 2>/dev/null || true
    docker-compose up -d || error_exit "Failed to start Docker containers"
    echo -e "${GREEN}✓ Containers started${NC}\n"

    echo -e "${YELLOW}Waiting for database...${NC}"
    DB_READY=false
    for i in {1..30}; do
        if docker exec underfrog-postgres pg_isready -U postgres > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Database ready${NC}\n"
            DB_READY=true
            break
        fi
        sleep 2
    done
    
    [ "$DB_READY" = true ] || error_exit "Database failed to start after 60 seconds"

    echo -e "${YELLOW}Creating tables...${NC}"
    
    # Array of SQL files to execute
    SQL_FILES=(
        "01-accounts.sql"
        "02-instruments.sql"
        "03-orders.sql"
        "04-positions.sql"
        "05-raw_prices.sql"
        "06-clean_prices.sql"
        "07-price_metrics.sql"
        "08-raw_stocks.sql"
        "09-raw_etfs.sql"
        "10-raw_bonds.sql"
        "11-clean_stocks.sql"
        "12-clean_etfs.sql"
        "13-clean_bonds.sql"
    )
    
    for sql_file in "${SQL_FILES[@]}"; do
        docker exec underfrog-postgres psql -U postgres -d underfrog -f /docker-entrypoint-initdb.d/"$sql_file" || error_exit "Failed to create table from $sql_file"
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

[ -f "db/seed/dummy-data.sql" ] || error_exit "db/seed/dummy-data.sql not found"
ACCOUNT_COUNT=$(docker exec underfrog-postgres psql -U postgres -d underfrog -Atc "SELECT COUNT(*) FROM accounts;") || error_exit "Failed to check existing accounts"

if [ "$ACCOUNT_COUNT" -gt 0 ]; then
    echo -e "${YELLOW}⚠ accounts already has $ACCOUNT_COUNT rows, skipping seed${NC}"
else
    docker exec -i underfrog-postgres psql -U postgres -d underfrog -v ON_ERROR_STOP=1 --single-transaction < db/seed/dummy-data.sql || error_exit "Failed to load db/seed/dummy-data.sql"
    echo -e "${GREEN}✓ Dummy data loaded${NC}"
fi

echo ""
echo -e "${GREEN}========================================="
echo "✓ Setup completed successfully!"
echo "=========================================${NC}"