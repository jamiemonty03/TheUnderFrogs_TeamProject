#!/bin/sh
# Loads the Demo Trader (ACC0011) history: three portfolios, filled/rejected/cancelled/open orders,
# trade history and Stripe top-ups. setup-pipeline.sh runs this; run it yourself to reset the demo account.
# Replaces only ACC0011's orders, trades, positions and cash movements; other accounts are untouched.
set -e
cd "$(dirname "$0")/../.."

for service in accounts orders positions; do
  echo "Loading demo data into ${service}-db"
  docker exec -i "${service}-db" sh -c \
    'psql -q -v ON_ERROR_STOP=1 -U "${SPRING_DATASOURCE_USERNAME:-postgres}" -d "${SPRING_DATASOURCE_URL##*/}"' \
    < "app/${service}-service/db/seed/seed-demo-trader.sql"
done

echo "Done. Sign in as demo to see the portfolio."
