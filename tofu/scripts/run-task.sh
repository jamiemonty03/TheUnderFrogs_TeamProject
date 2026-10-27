#!/usr/bin/env bash
# Runs a one-off Fargate task, waits for it to stop and exits non-zero if its container failed.
# Usage: run-task.sh <cluster> <subnet,subnet> <security group> <task definition arn>
set -euo pipefail

CLUSTER="$1"
SUBNETS="$2"
SECURITY_GROUP="$3"
TASK_DEFINITION="$4"
TIMEOUT_SECONDS=3600

TASK=$(aws ecs run-task \
  --cluster "$CLUSTER" \
  --task-definition "$TASK_DEFINITION" \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNETS],securityGroups=[$SECURITY_GROUP],assignPublicIp=DISABLED}" \
  --query 'tasks[0].taskArn' --output text)
[ -n "$TASK" ] && [ "$TASK" != "None" ] || { echo "Failed to start $TASK_DEFINITION" >&2; exit 1; }
echo "Started $TASK"

waited=0
while [ "$(aws ecs describe-tasks --cluster "$CLUSTER" --tasks "$TASK" --query 'tasks[0].lastStatus' --output text)" != "STOPPED" ]; do
  [ "$waited" -lt "$TIMEOUT_SECONDS" ] || { echo "Timed out waiting for $TASK" >&2; exit 1; }
  sleep 15
  waited=$((waited + 15))
done

read -r EXIT_CODE REASON <<< "$(aws ecs describe-tasks --cluster "$CLUSTER" --tasks "$TASK" \
  --query 'tasks[0].[containers[0].exitCode, stoppedReason]' --output text)"

if [ "$EXIT_CODE" != "0" ]; then
  echo "Task $TASK failed (exit code $EXIT_CODE): $REASON" >&2
  echo "Logs are in CloudWatch under /ecs/$CLUSTER/" >&2
  exit 1
fi
echo "Task $TASK finished"
