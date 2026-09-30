#!/bin/bash
set -e

BOOTSTRAP=kafka:9092
TOPICS=/opt/kafka/bin/kafka-topics.sh
DAY=86400000 

create() {
  "$TOPICS" --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
    --topic "$1" --partitions "$2" --replication-factor 1 \
    --config retention.ms="$3"
  echo "ready: $1"
}

# PLACEHOLDER
#      topic             partitions  retention
create orders            3           $((7 * DAY))
create trade-events      3           $((7 * DAY))
create market-data       3           $((1 * DAY))
create orders.DLT        3           $((14 * DAY))
create trade-events.DLT  3           $((14 * DAY))
create market-data.DLT   3           $((14 * DAY))

"$TOPICS" --bootstrap-server "$BOOTSTRAP" --describe
