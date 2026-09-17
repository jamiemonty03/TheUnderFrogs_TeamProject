# Sell Order – UML Sequence Diagram

Traced from `OrderService.placeOrder` / `OrderValidationService` for a `SELL`
order. Reflects current code, not the intended full trade lifecycle — the order
gets validated and recorded, but nothing else happens yet (see note at the end).
`Client` is illustrative — nothing in the codebase actually calls this flow yet
(`Main.java` is still a placeholder).

```mermaid
sequenceDiagram
    participant Client
    participant OrderService
    participant OrderValidationService
    participant PositionRepository

    Client->>OrderService: placeOrder(SELL order)
    OrderService->>OrderValidationService: validateOrder(...)
    Note right of OrderValidationService: Each check below stops the flow immediately on failure —<br/>only one exception can fire per call.

    rect rgb(255, 235, 235)
    opt account not ACTIVE
        OrderValidationService--xClient: AccountNotActiveException
    end
    end

    rect rgb(255, 235, 235)
    opt instrument missing or not tradable
        OrderValidationService--xClient: InstrumentNotFoundException / TradingException
    end
    end

    rect rgb(235, 245, 255)
    OrderValidationService->>PositionRepository: findByAccountAndSymbol(accountId, symbol)
    PositionRepository-->>OrderValidationService: Optional Position (empty if none)
    OrderValidationService->>OrderValidationService: held quantity = position.map(getQuantity).orElse(0)
    end

    rect rgb(255, 235, 235)
    opt held quantity < requested quantity
        OrderValidationService--xClient: InsufficientHoldingsException
    end
    end

    OrderValidationService-->>OrderService: validation passed
    OrderService->>OrderService: createOrder(order)

    rect rgb(255, 235, 235)
    opt idempotency key already used
        OrderService--xClient: DuplicateOrderException
    end
    end

    rect rgb(235, 255, 235)
    OrderService-->>Client: Order returned
    end
    Note over OrderService,Client: No execution yet: status stays<br/>null, Position/Account untouched.
```
