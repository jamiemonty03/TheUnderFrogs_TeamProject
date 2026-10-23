# Every service's schema and seed SQL, plus the script that loads them into RDS.
# Built from the repo root: docker build -f tofu/images/db-init/Dockerfile .
# Each folder under /db is named after the database it fills.
FROM postgres:15-alpine

COPY app/accounts-service/db/schema         /db/accounts_db/schema
COPY app/accounts-service/db/seed           /db/accounts_db/seed
COPY app/instruments-service/db/schema      /db/instruments_db/schema
COPY app/instruments-service/db/seed        /db/instruments_db/seed
COPY app/orders-service/db/schema           /db/orders_db/schema
COPY app/orders-service/db/seed             /db/orders_db/seed
COPY app/positions-service/db/schema        /db/positions_db/schema
COPY app/positions-service/db/seed          /db/positions_db/seed
COPY app/auth-service/db/schema             /db/auth_db/schema
COPY app/auth-service/db/seed               /db/auth_db/seed
COPY app/trade-analytics-service/db/schema  /db/analytics_db/schema
COPY tofu/images/db-init/db-init.sh         /db-init.sh

ENTRYPOINT ["sh", "/db-init.sh"]
