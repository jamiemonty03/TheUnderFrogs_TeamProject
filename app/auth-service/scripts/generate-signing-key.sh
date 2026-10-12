#!/bin/bash
set -euo pipefail

ENV_FILE="$(cd "$(dirname "$0")/.." && pwd)/.env"
[ -f "$ENV_FILE" ] || { echo "Missing $ENV_FILE, copy .env.example to .env first" >&2; exit 1; }

if grep -q '^JWT_PRIVATE_KEY=.' "$ENV_FILE" && [ "${1:-}" != "--rotate" ]; then
  echo "JWT_PRIVATE_KEY is already set in .env (run with --rotate to replace it)"
  exit 0
fi

KEY=$(docker run --rm node:24 sh -c 'openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 2>/dev/null | base64 -w0')
[ -n "$KEY" ] || { echo "Failed to generate a key" >&2; exit 1; }
KID="auth-$(date -u +%Y%m%d%H%M%S)"

UPDATED=$(grep -v '^JWT_PRIVATE_KEY=\|^JWT_KEY_ID=' "$ENV_FILE" || true)
printf '%s\nJWT_PRIVATE_KEY=%s\nJWT_KEY_ID=%s\n' "$UPDATED" "$KEY" "$KID" > "$ENV_FILE"

echo "Wrote a new 2048-bit RSA signing key to .env (kid: $KID)"
