#!/bin/sh
# Loads the Demo Trader (ACC0011) history into databases that already exist.
# New databases get it automatically from each service's db/seed/seed-demo-trader.sql.
# Replaces only ACC0011's orders, trades, positions and cash movements; other accounts are untouched.
set -e
cd "$(dirname "$0")/../.."

for service in accounts orders positions; do
  echo "Loading demo data into ${service}-db"
  docker exec -i "${service}-db" psql -q -v ON_ERROR_STOP=1 -U postgres -d "${service}_db" \
    < "app/${service}-service/db/seed/seed-demo-trader.sql"
done

echo "Done. Sign in as demo to see the portfolio."
