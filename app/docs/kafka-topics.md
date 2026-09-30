# Kafka Topics

Services send events to each other through Kafka instead of calling each other over REST.
This page is the team's reference for which topics exist, what goes on them, and why they are set up this way.

> **Status:** partition counts and retention are the team's proposal.
> If they change, update `kafka/create-topics.sh` and this page together.

## Topics

| Topic | Key | Partitions | Retention | DLT | Event types | Producers | Consumers |
|---|---|---|---|---|---|---|---|
| `orders` | `accountId` | 3 | 7 days | `orders.DLT` | `ORDER_PLACED` | orders-service | trade-executor |
| `trade-events` | `accountId` | 3 | 7 days | `trade-events.DLT` | `ORDER_FILLED`, `ORDER_REJECTED`, `ORDER_CANCELLED` | trade-executor | orders-service, cash/position updates, batch ETL, dashboard |
| `market-data` | `symbol` | 3 | 1 day | `market-data.DLT` | `PRICE_UPDATED` | market-data poller (inside trade-executor) | trade-executor, dashboard |
| `orders.DLT` | same as source | 3 | 14 days | n/a | failed `orders` messages | Kafka error handler | team (manual review) |
| `trade-events.DLT` | same as source | 3 | 14 days | n/a | failed `trade-events` messages | Kafka error handler | team (manual review) |
| `market-data.DLT` | same as source | 3 | 14 days | n/a | failed `market-data` messages | Kafka error handler | team (manual review) |

Topic names are defined once in `Topics.java` and event types in `EventTypes.java`. Use the constants; never type the names by hand.

## Why these keys

Kafka always sends messages with the same key to the same partition, and keeps them in order within that partition.

- **`orders` and `trade-events` are keyed by `accountId`.** All of one account's orders and trade results stay in the order they happened. This matters because each trade changes the account's cash and holdings: a sell must not be processed before the buy it depends on. Different accounts can still be processed in parallel.
- **`market-data` is keyed by `symbol`.** Each instrument's prices arrive in order, so a consumer never replaces a newer price with an older one.

## Why these partition counts

- **3 partitions per main topic.** Partitions are how Kafka spreads work across consumers: up to 3 copies of a consumer can each read one partition in parallel. We run a single broker for a small project, so 3 gives room to scale later without extra overhead. Partitions can be added later but never removed, and adding them moves keys to different partitions, so we chose a modest number up front.
- **`orders` and `trade-events` use the same count and the same key**, so an account's order and its result follow the same partition layout.
- **Each DLT has the same partition count as its source topic.** Spring Kafka's dead-letter handling sends a failed message to the same partition number in the `.DLT` topic, so the counts must match.

## Why these retention periods

Kafka is not our system of record (orders are stored in Postgres). Retention decides how far back a consumer can catch up or replay.

- **`orders` and `trade-events`: 7 days.** A consumer that was down, or the batch ETL job, can still catch up after a long outage such as a weekend.
- **`market-data`: 1 day.** Prices go stale within seconds and there are many of them, so keeping them longer only wastes disk space.
- **DLTs: 14 days.** Gives the team time to investigate and fix failed messages before they are deleted.

## Message envelope

Every message on every topic uses the same five-field envelope:

| Field | Meaning |
|---|---|
| `eventId` | Unique ID (UUID). Consumers use it to skip duplicate deliveries. |
| `eventType` | What happened, from `EventTypes.java` |
| `occurredAt` | When it happened (ISO-8601 UTC timestamp) |
| `key` | The Kafka key: `accountId` or `symbol` |
| `payload` | The event-specific data |

- Java: `app/orders-service/src/main/java/com/neueda/orderservice/events/EventEnvelope.java`. Other Java services copy the `events` package unchanged.
- Python and anything else: `app/docs/event-envelope.schema.json`. This schema is the official definition; every copy must match it.

## Dead-letter topics

If a consumer cannot process a message (for example, a missing field or an unknown symbol), it retries a few times, then moves the message to the matching `.DLT` topic and carries on. The bad message is kept for review, and it does not block the messages behind it.

## Connecting

| From | Address |
|---|---|
| Another container (the services) | `kafka:9092` |
| This machine, for testing | `localhost:9094` |

Topics are created by the one-shot `kafka-init` container (`kafka/create-topics.sh`) when the stack starts. Automatic topic creation is turned off, so a misspelt topic name fails with an error instead of silently creating a new topic.

A service that uses Kafka should wait for the topics in `docker-compose.yml`:

```yaml
depends_on:
  kafka-init:
    condition: service_completed_successfully
```

To list the topics and their settings:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe
```


## Inspecting and replaying `orders.DLT`

The Trade Executor sends malformed events, invalid order payloads, and unknown order IDs to `orders.DLT` immediately. Other processing failures are retried after 1, 2, 4, and 8 seconds (about 15 seconds total); the record is then sent to the DLT. Processing is bounded, so the consumer resumes the partition after recovering a failed record.

DLT records preserve the original key and payload. Spring Kafka adds `kafka_dlt-exception-fqcn` and `kafka_dlt-exception-message` headers with the failure type and reason; `x-attempt-count` records the attempt number (1 for immediate permanent failures). Inspect a record from the host with:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic orders.DLT --from-beginning \
  --property print.key=true --property print.headers=true
```

After investigating and fixing the cause, replay selected records by producing their original JSON payload and key back to `orders` (use the host listener `localhost:9094`). For example, copy the payload and key from the DLT output, then run:

```bash
docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic orders \
  --property parse.key=true --property key.separator=:
```

Enter one `key:payload` record per line, then press Ctrl-D. Replay only after confirming the failure is resolved; settlement calls are idempotent, so replaying a partially settled order is safe.
