#!/bin/bash
set -euo pipefail

usage() {
  echo "Usage: $0 <client-id> <client .env file> [--secret-only | --rotate]" >&2
  exit 1
}

[ $# -ge 2 ] || usage
CLIENT_ID="$1"
ENV_FILE="$2"
MODE="${3:-}"
[ -f "$ENV_FILE" ] || { echo "Missing $ENV_FILE, copy its .env.example first" >&2; exit 1; }

if [ "$MODE" = "--rotate" ] || ! grep -q '^AUTH_CLIENT_SECRET=.' "$ENV_FILE"; then
  SECRET=$(docker run --rm node:24 node -e 'console.log(require("crypto").randomBytes(32).toString("base64url"))')
  [ -n "$SECRET" ] || { echo "Failed to generate a client secret" >&2; exit 1; }
  UPDATED=$(grep -v '^AUTH_CLIENT_ID=\|^AUTH_CLIENT_SECRET=' "$ENV_FILE" || true)
  printf '%s\nAUTH_CLIENT_ID=%s\nAUTH_CLIENT_SECRET=%s\n' "$UPDATED" "$CLIENT_ID" "$SECRET" > "$ENV_FILE"
  echo "Wrote a new client secret for $CLIENT_ID to $ENV_FILE"
fi

[ "$MODE" = "--secret-only" ] && exit 0

SECRET=$(grep '^AUTH_CLIENT_SECRET=' "$ENV_FILE" | tail -1 | cut -d= -f2-)

HASH=$(printf '%s' "$SECRET" | docker exec -i auth-service node -e '
require("reflect-metadata");
const { PasswordHasher } = require("./dist/modules/users/password-hasher.service.js");
let secret = "";
process.stdin.on("data", (chunk) => (secret += chunk)).on("end", async () => console.log(await new PasswordHasher().hash(secret)));
')
[[ "$HASH" == \$argon2id\$* ]] || { echo "Failed to hash the client secret" >&2; exit 1; }

docker exec -i auth-db sh -c 'psql -q -v ON_ERROR_STOP=1 -U "$DB_USERNAME" -d "$DB_NAME" -v client_id="$1" -v secret_hash="$2"' _ "$CLIENT_ID" "$HASH" <<'SQL'
INSERT INTO service_clients (client_id, client_secret_hash)
VALUES (:'client_id', :'secret_hash')
ON CONFLICT (client_id) DO UPDATE
SET client_secret_hash = EXCLUDED.client_secret_hash, is_active = true, updated_at = NOW();
SQL

echo "Registered service client $CLIENT_ID in auth-db"
