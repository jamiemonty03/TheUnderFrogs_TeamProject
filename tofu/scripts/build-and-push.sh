#!/usr/bin/env bash
# Builds every image the stack runs and pushes it to ECR. Run by `tofu apply` when image_tag changes.
# Usage: build-and-push.sh <registry> <repo prefix> <tag>
set -euo pipefail

REGISTRY="$1"
PREFIX="$2"
TAG="$3"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY"

# The Java Dockerfiles copy target/<service>.jar, so build the jars first, as Jenkins does.
for svc in accounts-service instruments-service orders-service positions-service trade-executor; do
  echo "Building $svc jar"
  mvn -q -B -f "app/$svc/pom.xml" clean package -Dmaven.test.skip=true
done

# build <image name> <context> [dockerfile]
build() {
  local image="$REGISTRY/$PREFIX/$1:$TAG"
  echo "Building $image"
  docker build --platform linux/amd64 -t "$image" -f "${3:-$2/Dockerfile}" "$2"
  docker push "$image"
}

for svc in accounts-service instruments-service orders-service positions-service trade-executor \
           auth-service trade-analytics-service frontend; do
  build "$svc" "app/$svc"
done

# These two build from the repo root; each Dockerfile has a .dockerignore beside it naming what it needs.
build python . tofu/images/python/Dockerfile
build db-init . tofu/images/db-init/Dockerfile
