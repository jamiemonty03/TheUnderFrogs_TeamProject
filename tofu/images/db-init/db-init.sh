#!/bin/sh
# Does on RDS what the compose postgres containers do on a fresh volume: create each database,
# then load its schema/*.sql and seed/*.sql, in name order, once. A marker table records that a
# database is done, so running this again changes nothing.
# Connection settings come from PGHOST, PGUSER, PGPASSWORD and PGDATABASE.
set -eu

MARKER=deploy_initialised

for dir in /db/*/; do
  db=$(basename "$dir")

  if [ "$(psql -Atc "SELECT 1 FROM pg_database WHERE datname = '$db'")" != "1" ]; then
    echo "Creating database $db"
    psql -v ON_ERROR_STOP=1 -c "CREATE DATABASE $db"
  fi

  if [ "$(psql -d "$db" -Atc "SELECT to_regclass('public.$MARKER') IS NOT NULL")" = "t" ]; then
    echo "$db is already initialised, skipping"
    continue
  fi

  set --
  for f in "$dir"schema/*.sql "$dir"seed/*.sql; do
    [ -f "$f" ] || continue
    echo "Loading $f"
    set -- "$@" -f "$f"
  done

  # One transaction per database, so a failure leaves nothing half-loaded and the next run retries cleanly.
  psql -v ON_ERROR_STOP=1 --single-transaction -d "$db" "$@" \
    -c "CREATE TABLE public.$MARKER (initialised_at timestamptz NOT NULL DEFAULT now())"
  echo "Initialised $db"
done
