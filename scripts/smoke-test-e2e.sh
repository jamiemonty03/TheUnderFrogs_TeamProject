#!/bin/bash
# End-to-end smoke test: place a BUY order and check it gets FILLED and settled.
# Usage: ./scripts/smoke-test-e2e.sh   (KEEP_UP=true leaves the stack running)
set -euo pipefail

RUN_ID=$(date +%s)
USERNAME="smoke$RUN_ID"
SYMBOL=AAPL
LIMIT=100000
QTY=1
STARTING_CASH=100000
SERVICES="auth-service accounts-service instruments-service orders-service positions-service trade-executor"

cd "$(dirname "$0")/.."

teardown() {
    if [ "${KEEP_UP:-false}" != "true" ]; then
        docker-compose down --volumes
    fi
}
trap teardown EXIT

fail() { echo "FAIL: $1" >&2; exit 1; }
json() { python3 -c "import sys, json; print(json.load(sys.stdin)$1)"; }

echo "== Starting the stack"
./app/auth-service/scripts/generate-signing-key.sh
docker-compose up -d --build --wait $SERVICES

echo "== Loading market data"
docker-compose up -d python trade-analytics-db
./db/scripts/setup-pipeline.sh populate
docker-compose stop python trade-analytics-service

echo "== Registering $USERNAME through auth-service"
REGISTER=$(curl -sf -X POST http://localhost:8087/auth/register -H 'Content-Type: application/json' \
    -d "{\"username\":\"$USERNAME\",\"email\":\"$USERNAME@example.com\",\"password\":\"$(openssl rand -hex 16)\",\"full_name\":\"Smoke Test\"}") \
    || fail "could not register $USERNAME with auth-service"
TOKEN=$(echo "$REGISTER" | json "['accessToken']")
ACCOUNT=$(echo "$REGISTER" | json "['accountId']")
AUTH="Authorization: Bearer $TOKEN"

echo "== Funding account $ACCOUNT with $STARTING_CASH"
docker exec accounts-db sh -c 'psql -q -v ON_ERROR_STOP=1 -U "$SPRING_DATASOURCE_USERNAME" -d "${SPRING_DATASOURCE_URL##*/}" -c "$1"' _ \
    "UPDATE accounts SET cash_balance = $STARTING_CASH WHERE account_id = '$ACCOUNT'" \
    || fail "could not fund account $ACCOUNT"

echo "== Placing BUY $QTY $SYMBOL"
RESPONSE=$(curl -s -w '\n%{http_code}' -X POST http://localhost:8083/api/orders -H "$AUTH" -H 'Content-Type: application/json' \
    -d "{\"accountId\":\"$ACCOUNT\",\"symbol\":\"$SYMBOL\",\"side\":\"BUY\",\"quantity\":$QTY,\"priceLimit\":$LIMIT,\"idempotencyKey\":\"smoke-$RUN_ID\"}")
STATUS_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n -1)
[ "$STATUS_CODE" = "201" ] || fail "POST /orders returned $STATUS_CODE: $BODY"
ORDER_ID=$(echo "$BODY" | json "['orderId']")
[ "$(echo "$BODY" | json "['orderStatus']")" = "NEW" ] || fail "order $ORDER_ID was not NEW"
echo "Order $ORDER_ID placed"

echo "== Waiting for the fill"
for _ in $(seq 30); do
    STATUS=$(curl -sf -H "$AUTH" "http://localhost:8083/api/orders/$ORDER_ID" | json "['orderStatus']")
    if [ "$STATUS" != "NEW" ]; then
        break
    fi
    sleep 1
done
[ "$STATUS" = "FILLED" ] || fail "order $ORDER_ID ended as $STATUS, expected FILLED"

echo "== Checking trade-events"
FILLED_EVENTS=$(docker exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
    --topic trade-events --from-beginning --timeout-ms 5000 2>/dev/null | grep "\"orderId\":\"$ORDER_ID\"" | grep ORDER_FILLED || true)
[ "$(echo "$FILLED_EVENTS" | grep -c .)" = "1" ] || fail "expected exactly one ORDER_FILLED for $ORDER_ID"
FILL_PRICE=$(echo "$FILLED_EVENTS" | json "['payload']['fillPrice']")

echo "== Checking cash and position"
CASH=$(curl -sf -H "$AUTH" "http://localhost:8081/api/accounts/$ACCOUNT" | json "['cashBalance']")
POSITION=$(curl -sf -H "$AUTH" "http://localhost:8084/api/positions/$ACCOUNT/$SYMBOL" | json "['quantity']") \
    || fail "no $SYMBOL position for $ACCOUNT"
python3 - "$STARTING_CASH" "$CASH" "$POSITION" "$FILL_PRICE" "$QTY" <<'EOF'
import sys
from decimal import Decimal
starting_cash, cash, position, price, qty = map(Decimal, sys.argv[1:])
assert cash == starting_cash - price * qty, f"cash is {cash}, expected {starting_cash - price * qty}"
assert position == qty, f"position is {position}, expected {qty}"
EOF

echo "== Checking market-data (soft)"
if docker exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
    --topic market-data --from-beginning --max-messages 1 --timeout-ms 15000 >/dev/null 2>&1; then
    echo "market-data has PRICE_UPDATED events"
else
    echo "WARN: no market-data events (no Alpaca keys, or the US market is closed)"
fi

echo "PASS: order $ORDER_ID filled at $FILL_PRICE and settled"
