# Idempotent Writes – UML Sequence Diagrams

Traced from the `feature/idempotentWrites` branch. These are the endpoints the
Trade Executor will call when it settles an order. Each one is now safe to
repeat: called twice with the same `orderId`, it changes the data once.

Cash lives in accounts-db, positions in positions-db and orders in orders-db,
so the three writes can't share one transaction. Each service makes its own
write safe to repeat instead, and the saga story (S7-5) will chain them.

`Caller` is the Trade Executor (service token, role `SERVICE`) or an
authenticated user. Nothing calls these flows automatically yet.

Colour key: blue = normal path, green = repeat (nothing changes),
red = rejected or rolled back.
Kafka topics (diagrams 6 and 7): yellow box = `orders`, purple box =
`trade-events`, red box = `orders.DLT`. All topics have 3 partitions.

---

## 1. Debit / credit with an orderId (accounts-service)

`AccountService.debit(accountId, amount, orderId)`. Credit is the same shape,
with `MovementType.CREDIT` and no funds check.

```mermaid
sequenceDiagram
    participant Caller
    participant AccountController
    participant AccountService
    participant CashMovementRepository
    participant AccountRepository
    participant DB as accounts-db

    Caller->>AccountController: POST /accounts/{id}/debit {amount, orderId}
    AccountController->>AccountService: debit(accountId, amount, orderId)
    Note over AccountService,DB: @Transactional(rollbackFor = Exception.class)<br/>ledger insert and balance update commit together or not at all

    AccountService->>AccountService: amount > 0 ? else IllegalArgumentException (422)
    AccountService->>AccountRepository: findById(accountId)
    AccountRepository-->>AccountService: Account (else AccountNotFoundException, 404)

    AccountService->>CashMovementRepository: insertIfAbsent(orderId, DEBIT, accountId, amount)
    CashMovementRepository->>DB: INSERT INTO cash_movements ...<br/>ON CONFLICT (order_id, movement_type) DO NOTHING
    DB-->>CashMovementRepository: rows inserted (1 or 0)

    rect rgb(235, 255, 235)
    opt 0 rows: this order was already debited
        AccountService-->>Caller: 200 current balance (nothing changed)
    end
    end

    rect rgb(255, 235, 235)
    opt account not ACTIVE
        AccountService--xCaller: AccountNotActiveException (403), ledger row rolled back
    end
    opt balance < amount
        AccountService--xCaller: InsufficientFundsException (400), ledger row rolled back
    end
    end

    rect rgb(235, 245, 255)
    AccountService->>AccountService: applyChange: balance - amount, lastUpdated, version + 1
    AccountService->>AccountRepository: save(account)
    AccountRepository->>DB: UPDATE accounts
    AccountService-->>Caller: 200 new balance
    end
```

Without an `orderId`, `isRepeat` returns false and no ledger row is written:
the call behaves exactly as before (every call is applied).

The ledger check runs **before** the funds check. Otherwise a repeated debit
would fail with "insufficient funds", because the first call already lowered
the balance.

---

## 2. Reversal (accounts-service)

`AccountService.reverse(accountId, orderId)`. Used by the saga to undo a step.

```mermaid
sequenceDiagram
    participant Caller
    participant AccountService
    participant CashMovementRepository
    participant AccountRepository

    Caller->>AccountService: POST /accounts/{id}/reversal {orderId}
    AccountService->>AccountRepository: findById(accountId)
    AccountService->>CashMovementRepository: findByOrderId(orderId)
    CashMovementRepository-->>AccountService: movements (REVERSAL rows filtered out)

    rect rgb(235, 255, 235)
    opt no DEBIT or CREDIT for this order
        AccountService-->>Caller: 200 unchanged (safe to undo a step that never happened)
    end
    end

    rect rgb(255, 235, 235)
    opt more than one movement, or it belongs to another account
        AccountService--xCaller: IllegalArgumentException (422)
    end
    end

    AccountService->>CashMovementRepository: insertIfAbsent(orderId, REVERSAL, accountId, original amount)

    rect rgb(235, 255, 235)
    opt 0 rows: already reversed
        AccountService-->>Caller: 200 current balance (nothing changed)
    end
    end

    rect rgb(235, 245, 255)
    AccountService->>AccountService: DEBIT reversed: balance + amount<br/>CREDIT reversed: balance - amount
    AccountService->>AccountRepository: save(account)
    AccountService-->>Caller: 200 new balance
    end
```

No `ACTIVE` check on a reversal: undoing a step must still work if the account
was suspended in between.

---

## 3. Buy / sell / reversal (positions-service)

`PositionService.updatePositionAfterBuy/Sell(..., orderId)` and `reverse(...)`.
Same pattern as accounts, with one extra rule: the ledger stores a `price` for
every movement, so a reversal can restore the cost basis.

| Movement | `price` stored in the ledger |
|---|---|
| BUY | The purchase price |
| SELL | The position's average cost at the moment of the sale |

```mermaid
sequenceDiagram
    participant Caller
    participant PositionService
    participant PositionMovementRepository
    participant PositionRepository

    Caller->>PositionService: POST /positions/{acc}/{sym}/sell {quantity, orderId}
    PositionService->>PositionRepository: findById(acc, sym)
    PositionRepository-->>PositionService: Position (average cost read here)
    PositionService->>PositionMovementRepository: insertIfAbsent(orderId, SELL, acc, sym, qty, averageCost)

    rect rgb(235, 255, 235)
    opt 0 rows: already sold for this order
        PositionService-->>Caller: 200 current position (quantity 0 if fully sold)
    end
    end

    rect rgb(255, 235, 235)
    opt held quantity < requested
        PositionService--xCaller: InsufficientHoldingsException (409), ledger row rolled back
    end
    end

    rect rgb(235, 245, 255)
    PositionService->>PositionService: quantity - sold
    alt quantity now 0
        PositionService->>PositionRepository: deleteById(acc, sym)
    else shares left
        PositionService->>PositionRepository: save(position)
    end
    PositionService-->>Caller: 200 position
    end

    Note over Caller,PositionRepository: Later, the saga undoes this sell

    Caller->>PositionService: POST /positions/{acc}/{sym}/reversal {orderId}
    PositionService->>PositionMovementRepository: findByOrderId(orderId)
    PositionMovementRepository-->>PositionService: SELL movement (qty, average cost at sale)
    PositionService->>PositionMovementRepository: insertIfAbsent(orderId, REVERSAL, ...)

    rect rgb(235, 245, 255)
    PositionService->>PositionService: addShares(qty, recorded average cost)<br/>recreates the position if it was deleted
    PositionService->>PositionRepository: save(position)
    PositionService-->>Caller: 200 position restored
    end
```

Reversing a **buy** works the other way: `removeShares` takes the shares and
their cost out of the average, deletes the position if nothing is left, and
returns 409 if those shares have since been sold.

---

## 4. Guarded status change (orders-service)

`PATCH /orders/{id}/status` → `OrderStatusService.changeStatus(...)`. This is
now the **only** way to change an order's status.

```mermaid
sequenceDiagram
    participant Caller
    participant OrderController
    participant OrderStatusService
    participant OrderRepository
    participant DB as orders-db

    Caller->>OrderController: PATCH /orders/{id}/status {expectedStatus, newStatus, reason}
    OrderController->>OrderStatusService: changeStatus(id, expected, new, reason)

    rect rgb(255, 235, 235)
    opt not NEW → FILLED, REJECTED or CANCELLED
        OrderStatusService--xCaller: IllegalArgumentException (422 VAL-422), database not touched
    end
    end

    OrderStatusService->>OrderRepository: updateStatusIfCurrent(id, expected, new)
    OrderRepository->>DB: UPDATE orders SET order_status = new, version = version + 1<br/>WHERE order_id = id AND order_status = expected
    DB-->>OrderRepository: rows updated (1 or 0)
    OrderStatusService->>OrderRepository: findById(id)

    rect rgb(255, 235, 235)
    opt order does not exist
        OrderStatusService--xCaller: OrderNotFoundException (404 ORD-404)
    end
    opt 0 rows: order is no longer in expectedStatus
        OrderStatusService--xCaller: OrderStatusConflictException (409 ORD-409 + currentStatus)
    end
    end

    rect rgb(235, 245, 255)
    OrderStatusService->>OrderStatusService: log reason (no column for it)
    OrderStatusService-->>Caller: 200 updated order
    end
```

A caller that gets **409 with `currentStatus` equal to the status it asked
for** can treat the change as already done. That is what makes this endpoint
safe to repeat.

### Why one guarded statement: two callers racing

```mermaid
sequenceDiagram
    participant Trader
    participant Executor as Trade Executor
    participant DB as orders-db

    Note over Trader,DB: Order is NEW
    Trader->>DB: UPDATE ... SET CANCELLED WHERE status = NEW
    Executor->>DB: UPDATE ... SET FILLED WHERE status = NEW
    DB-->>Trader: 1 row → 200 CANCELLED
    DB-->>Executor: 0 rows → 409 currentStatus CANCELLED
    Note over Executor: Executor knows not to take the cash or add the shares
```

The database runs the two updates one at a time, so exactly one wins and the
other is told. A "read the status in Java, then save" approach would let both
pass the check.

---

## 5. PUT can no longer change the status (orders-service)

```mermaid
sequenceDiagram
    participant Caller
    participant OrderController
    participant Validation as Bean Validation

    Caller->>OrderController: PUT /orders/{id} {"orderStatus": "NEW"}
    OrderController->>Validation: @Valid UpdateOrderRequest
    rect rgb(255, 235, 235)
    Validation--xCaller: @Null on orderStatus fails → 422 VAL-422<br/>"use PATCH /orders/{id}/status"
    end

    Caller->>OrderController: PUT /orders/{id} {"quantity": 2}
    rect rgb(235, 245, 255)
    OrderController->>OrderController: update quantity / price / side only
    OrderController-->>Caller: 200, status unchanged
    end
```

`updateOrder` also no longer copies `orderStatus` at all, so even code that
calls it directly (skipping validation) can't change the status.

---

## 6. How the saga (S7-5) will use these (future)

Not built in this branch. This shows where Kafka fits and why every step needs
an `orderId` and a reversal: Kafka can deliver the same `ORDER_PLACED` twice,
so the executor must be able to repeat every call safely.

```mermaid
sequenceDiagram
    participant Orders as orders-service

    box rgb(255, 243, 214) Kafka topic: orders (3 partitions, key = accountId)
        participant OrdersTopic as orders<br/>partition = hash(accountId) % 3
    end

    participant Executor as Trade Executor<br/>(group: trade-executor)
    participant Accounts as accounts-service
    participant Positions as positions-service

    box rgb(232, 222, 248) Kafka topic: trade-events (3 partitions, key = accountId)
        participant TradeEvents as trade-events<br/>same partition rule
    end

    Orders->>OrdersTopic: ORDER_PLACED (key = accountId)
    OrdersTopic->>Executor: deliver from that account's partition, in order

    Executor->>Accounts: POST /debit {amount, orderId}
    Executor->>Positions: POST /buy {quantity, price, orderId}
    Executor->>Orders: PATCH /status NEW → FILLED

    alt every step succeeds (or returns "already done")
        Executor->>TradeEvents: ORDER_FILLED (key = accountId)
        Note over Executor,TradeEvents: Order settled. If the executor crashes before acknowledging,<br/>Kafka redelivers ORDER_PLACED and every call above is repeated safely.
    else a step fails
        Executor->>Positions: POST /reversal {orderId}
        Executor->>Accounts: POST /reversal {orderId}
        Executor->>Orders: PATCH /status NEW → REJECTED
        Executor->>TradeEvents: ORDER_REJECTED (key = accountId)
        Note over Executor,TradeEvents: Each undo is also safe to repeat, and safe if the step never ran
    end

    Executor->>OrdersTopic: acknowledge (commit offset)
```

Using the same key (`accountId`) on `orders` and `trade-events` means one
account's order and its result always travel through the same partition
number, in order.

---

## 7. Kafka flow as implemented today

What actually runs now. orders-service publishes events, and the Trade Executor
consumes `orders` but only **logs** its decision: it doesn't call accounts or
positions yet, and doesn't change the order's status.

| Topic | Partitions | Key | Retention | Published by | Consumed by |
|---|---|---|---|---|---|
| **`orders`** | 3 | `accountId` | 7 days | orders-service (`ORDER_PLACED`) | Trade Executor |
| **`trade-events`** | 3 | `accountId` | 7 days | orders-service (`ORDER_CANCELLED`) | nobody yet |
| **`orders.DLT`** | 3 | same as source | 14 days | Trade Executor's error handler | nobody yet (manual review) |
| `market-data` (+ `.DLT`) | 3 | `symbol` | 1 day (14) | not used yet | not used yet |

### 7a. Placing an order → `orders` topic → Trade Executor

```mermaid
sequenceDiagram
    participant Client
    participant Orders as orders-service
    participant OrdersDB as orders-db

    box rgb(255, 243, 214) Kafka topic: orders (3 partitions, key = accountId, 7 days)
        participant P0 as orders<br/>partition 0
        participant P1 as orders<br/>partition 1
        participant P2 as orders<br/>partition 2
    end

    participant Executor as Trade Executor<br/>(group: trade-executor)
    participant OrdersAPI as orders-service API
    participant Instruments as instruments-service

    Client->>Orders: POST /orders {accountId: ACC0001, ...}
    Orders->>OrdersDB: INSERT order (status NEW)
    Orders-->>Client: 201 NEW (execution happens asynchronously)

    Note over Orders,P2: OrderEventPublisher runs AFTER_COMMIT:<br/>only orders that were really saved are published
    Orders->>P1: ORDER_PLACED in EventEnvelope, key = "ACC0001"
    Note over P0,P2: The partition is hash("ACC0001") % 3 (partition 1 here as an example).<br/>Every message for ACC0001 lands in the same partition,<br/>so one account's orders are read in the order they were placed.<br/>Other accounts spread across partitions 0 and 2.

    P1->>Executor: deliver ORDER_PLACED
    Note over Executor: ack mode MANUAL: the offset is only committed<br/>after processing, so a crash means redelivery

    rect rgb(235, 255, 235)
    opt eventType is not ORDER_PLACED
        Executor->>P1: acknowledge, skip
    end
    end

    rect rgb(235, 245, 255)
    Executor->>OrdersAPI: GET /orders/{orderId} (service token)
    OrdersAPI-->>Executor: order
    opt status is not NEW
        Executor->>P1: acknowledge, skip (already handled)
    end
    Executor->>Instruments: GET instrument + price (service token)
    Executor->>Executor: FillRule.decide → FILL or REJECT
    Executor->>Executor: LoggingSettlementService: log "Order ... FILL / REJECT"
    Executor->>P1: acknowledge (commit offset)
    end
```

### 7b. When processing fails → `orders.DLT`

```mermaid
sequenceDiagram
    box rgb(255, 243, 214) Kafka topic: orders
        participant P1 as orders<br/>partition 1
    end

    participant Executor as Trade Executor
    participant Handler as DefaultErrorHandler

    box rgb(255, 225, 225) Kafka topic: orders.DLT (3 partitions, 14 days)
        participant D1 as orders.DLT<br/>partition 1
    end

    P1->>Executor: ORDER_PLACED
    Executor--xHandler: exception (e.g. orders-service unreachable)

    alt retryable error
        loop retry with back-off: 1s, 2s, 4s, 8s (about 15s in total)
            Handler->>Executor: redeliver the same record
        end
        Handler->>D1: still failing → publish to orders.DLT,<br/>SAME partition number, header x-attempt-count
    else not retryable (bad JSON, deserialization,<br/>IllegalArgumentException, UnknownOrderException)
        Handler->>D1: publish to orders.DLT immediately
    end

    Handler->>P1: commit offset, so the next message is not blocked
    Note over P1,D1: The failed message is kept for 14 days for someone to inspect.<br/>The DLT has the same partition count as orders (3), because the recoverer<br/>reuses the original partition number.
```

### 7c. Cancelling an order → `trade-events` topic

```mermaid
sequenceDiagram
    participant Client
    participant Orders as orders-service
    participant OrdersDB as orders-db

    box rgb(232, 222, 248) Kafka topic: trade-events (3 partitions, key = accountId, 7 days)
        participant T as trade-events<br/>partition = hash(accountId) % 3
    end

    Client->>Orders: DELETE /orders/{orderId}
    Orders->>OrdersDB: UPDATE ... SET CANCELLED WHERE status = NEW
    alt 1 row updated
        Orders-->>Client: 204
        Orders->>T: ORDER_CANCELLED, key = accountId (AFTER_COMMIT)
        Note over T: No consumer yet. The saga and dashboard stories will read it.
    else 0 rows (no longer NEW)
        Orders-->>Client: 409 ORD-409, nothing published
    end
```

**How this branch connects:** the endpoints from diagrams 1–5 are what the
Trade Executor will call in place of `LoggingSettlementService` (diagram 6).
Because Kafka can redeliver a message (manual ack, retries), those calls must
be safe to repeat, which is what this branch adds.

---

## Ledger tables

| Table | Service | Key rule |
|---|---|---|
| `cash_movements` | accounts-service | `UNIQUE (order_id, movement_type)`: one DEBIT, one CREDIT and one REVERSAL per order |
| `position_movements` | positions-service | `UNIQUE (order_id, movement_type)`: one BUY, one SELL and one REVERSAL per order |

Both start empty. Rows are only written when a call includes an `orderId`.
