# Order Placement – UML Sequence Diagram

Comprehensive sequence diagram tracing `OrderService.placeOrder()` through all validation steps. Shows both **BUY** and **SELL** order flows with validation checkpoints and potential exception paths.

The flow validates in strict sequence — validation stops immediately on first failure. Only successful validation leads to order creation.

## BUY Order Flow
```mermaid
sequenceDiagram
    participant Client
    participant OrderService
    participant OrderValidationService

    Client->>OrderService: placeOrder(account, instrument, BUY, quantity, price, idempKey)
    
    Note over OrderService: 1. Validate order inputs
    OrderService->>OrderValidationService: validateOrder(account, instrument, BUY, quantity, price)
    
    Note over OrderValidationService: 2. Check account is active
    rect rgb(255, 235, 235)
    alt account is null
        OrderValidationService--xClient: InvalidOrderException
    else account status != ACTIVE
        OrderValidationService--xClient: AccountNotActiveException
    end
    end

    Note over OrderValidationService: 3. Check instrument exists and tradable
    rect rgb(255, 235, 235)
    alt instrument is null
        OrderValidationService--xClient: InstrumentNotFoundException
    else instrument not tradable
        OrderValidationService--xClient: TradingException
    end
    end

    Note over OrderValidationService: 4. Validate BUY order specifics
    rect rgb(235, 245, 255)
    alt quantity <= 0
        OrderValidationService--xClient: InvalidOrderException
    else price <= 0
        OrderValidationService--xClient: InvalidOrderException
    else (quantity × price) > cash balance
        OrderValidationService--xClient: InsufficientFundsException
    end
    end

    Note over OrderService: 5. All validations passed
    OrderValidationService-->>OrderService: validation success

    Note over OrderService: 6. Create and store order
    OrderService->>OrderService: createOrder(order)
    
    rect rgb(255, 235, 235)
    alt idempotency key already used
        OrderService--xClient: DuplicateOrderException
    end
    end

    Note over OrderService: 7. Order created successfully
    OrderService-->>Client: Order (status=NEW)
```

