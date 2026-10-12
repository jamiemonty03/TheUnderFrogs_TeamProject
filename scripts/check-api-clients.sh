#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
GENERATED=app/frontend/src/app/api

docker run --rm -u "$(id -u):$(id -g)" -e HOME=/tmp -e npm_config_cache=/tmp/.npm \
  -v "$ROOT":/repo -w /repo/app/frontend node:24 \
  sh -c 'npm ci --no-audit --no-fund --loglevel=error && npm run generate:api --silent'

CHANGES="$(git -C "$ROOT" status --porcelain -- "$GENERATED")"
if [ -n "$CHANGES" ]; then
  echo "✗ The generated API clients are out of date with the OpenAPI specs:"
  echo "$CHANGES"
  git -C "$ROOT" --no-pager diff --stat -- "$GENERATED"
  echo "Run 'npm run generate:api' in app/frontend and commit the result."
  exit 1
fi
echo "✓ Generated API clients match the OpenAPI specs"
