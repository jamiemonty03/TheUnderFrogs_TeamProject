#!/bin/sh
# Postgres init hook (copied into /docker-entrypoint-initdb.d by docker-compose).
# Runs once, when a DB volume is first initialised, after the service's db/schema/*.sql files.
# Loads every file in the service's db/seed/ folder, mounted at /db/seed.
for f in /db/seed/*.sql; do
  [ -f "$f" ] || continue
  echo "Seeding $f"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -f "$f"
done
