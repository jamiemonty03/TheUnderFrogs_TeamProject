#!/bin/bash

set -e

echo "========================================="
echo "TheUnderFrogs ETL Pipeline Setup"
echo "========================================="

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

if [ "$1" == "--help" ] || [ "$1" == "-h" ]; then
    echo "Usage: ./setup-pipeline.sh [option]"
    echo ""
    echo "Options:"
    echo "  full        Build, create tables, and populate (default)"
    echo "  skip-build  Skip Maven build, just create tables and populate"
    echo "  populate    Only run population scripts (no table creation)"
    exit 0
fi

MODE="${1:-full}"

if [ "$MODE" != "full" ] && [ "$MODE" != "skip-build" ] && [ "$MODE" != "populate" ]; then
    echo -e "${RED}Invalid option: $MODE${NC}"
    echo "Use --help for usage information"
    exit 1
fi

if [ "$MODE" = "full" ] || [ "$MODE" = "skip-build" ]; then
    if [ "$MODE" = "full" ]; then
        echo -e "${YELLOW}Building Java application...${NC}"
        mvn clean package -Dmaven.test.skip=true
        echo -e "${GREEN}✓ Build complete${NC}\n"
    fi

    echo -e "${YELLOW}Starting Docker containers...${NC}"
    docker-compose down --remove-orphans 2>/dev/null || true
    docker-compose up -d

    echo -e "${YELLOW}Waiting for database...${NC}"
    for i in {1..30}; do
        if docker exec underfrog-postgres pg_isready -U postgres > /dev/null 2>&1; then
            echo -e "${GREEN}✓ Database ready${NC}\n"
            break
        fi
        sleep 2
    done

    echo -e "${YELLOW}Creating tables...${NC}"
    docker exec underfrog-postgres psql -U postgres -d underfrog -f /docker-entrypoint-initdb.d/01-accounts.sql
    docker exec underfrog-postgres psql -U postgres -d underfrog -f /docker-entrypoint-initdb.d/02-instruments.sql
    docker exec underfrog-postgres psql -U postgres -d underfrog -f /docker-entrypoint-initdb.d/03-orders.sql
    docker exec underfrog-postgres psql -U postgres -d underfrog -f /docker-entrypoint-initdb.d/04-positions.sql
    docker exec underfrog-postgres psql -U postgres -d underfrog -f /docker-entrypoint-initdb.d/05-raw_prices.sql
    docker exec underfrog-postgres psql -U postgres -d underfrog -f /docker-entrypoint-initdb.d/06-clean_prices.sql
    docker exec underfrog-postgres psql -U postgres -d underfrog -f /docker-entrypoint-initdb.d/07-price_metrics.sql
    docker exec underfrog-postgres psql -U postgres -d underfrog -f /docker-entrypoint-initdb.d/08-raw_stocks.sql
    docker exec underfrog-postgres psql -U postgres -d underfrog -f /docker-entrypoint-initdb.d/09-raw_etfs.sql
    docker exec underfrog-postgres psql -U postgres -d underfrog -f /docker-entrypoint-initdb.d/10-raw_bonds.sql
    docker exec underfrog-postgres psql -U postgres -d underfrog -f /docker-entrypoint-initdb.d/11-clean_stocks.sql
    docker exec underfrog-postgres psql -U postgres -d underfrog -f /docker-entrypoint-initdb.d/12-clean_etfs.sql
    docker exec underfrog-postgres psql -U postgres -d underfrog -f /docker-entrypoint-initdb.d/13-clean_bonds.sql
    echo -e "${GREEN}✓ Tables created${NC}\n"
else
    echo -e "${YELLOW}Skipping Docker startup (populate mode)${NC}\n"
fi

echo -e "${YELLOW}Populating tables...${NC}"
docker exec underfrog-dashboard python /sql/01-generate_raw_instruments.py
echo -e "${GREEN}✓ Instruments${NC}"
docker exec underfrog-dashboard python /sql/02-generate_raw_prices.py
echo -e "${GREEN}✓ Prices${NC}"
docker exec underfrog-dashboard python /sql/03-etl_prices.py
echo -e "${GREEN}✓ ETL Prices${NC}"
docker exec underfrog-dashboard python /sql/04-etl_stocks.py
echo -e "${GREEN}✓ ETL Stocks${NC}"
docker exec underfrog-dashboard python /sql/05-etl_etfs.py
echo -e "${GREEN}✓ ETL ETFs${NC}"
docker exec underfrog-dashboard python /sql/06-etl_bonds.py
echo -e "${GREEN}✓ ETL Bonds${NC}"
docker exec underfrog-dashboard python /sql/etl_instruments_metrics.py
echo -e "${GREEN}✓ ETL Instruments Metrics${NC}"
docker exec underfrog-dashboard python /sql/etl_price_metrics.py
echo -e "${GREEN}✓ ETL Price Metrics${NC}\n"

echo -e "${GREEN}========================================="
echo "Setup completed!"
echo "=========================================${NC}"