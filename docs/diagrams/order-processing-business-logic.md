# Order Processing – Business Logic Sequence Diagram

This diagram traces the current order-processing flow through `OrderProcessor`,
`OrderService`, validation, account cash movement, position updates, and final
order status. It covers both BUY and SELL execution paths.

Validation occurs before execution. An invalid request stops at
`OrderService.placeOrder()` and no strategy is executed. The repositories shown
here are the current in-memory persistence boundaries; database transactions are
planned for the Spring Boot implementation.

```mermaid
%%{init: {'sequence': {'mirrorActors': false}}}%%
sequenceDiagram
    actor Client
    participant OrderProcessor
    participant OrderService
    participant OrderValidationService
    participant AccountService
    participant PositionManager
    participant PositionService
    participant PositionRepository
    participant OrderExecutionStrategy

    Client->>+OrderProcessor: processOrder(account, instrument, side, quantity, price, idempotencyKey)
    OrderProcessor->>+OrderService: placeOrder(account, instrument, side, quantity, price, idempotencyKey)
    OrderService->>+OrderValidationService: validateOrder(account, instrument, side, quantity, price)

    Note over OrderValidationService: Validate account, instrument, quantity, price, and side

    alt account or instrument invalid
        OrderValidationService--xOrderService: AccountNotActiveException / InstrumentNotFoundException / TradingException
        OrderService--xOrderProcessor: validation failure
        OrderProcessor--xClient: rejected order
    else BUY order lacks sufficient cash
        OrderValidationService--xOrderService: InsufficientFundsException
        OrderService--xOrderProcessor: validation failure
        OrderProcessor--xClient: rejected order
    else SELL order lacks sufficient holdings
        OrderValidationService->>PositionRepository: findByAccountAndSymbol(accountId, symbol)
        PositionRepository-->>OrderValidationService: position or empty
        OrderValidationService--xOrderService: InsufficientHoldingsException
        OrderService--xOrderProcessor: validation failure
        OrderProcessor--xClient: rejected order
    else order input is invalid
        OrderValidationService--xOrderService: InvalidOrderException
        OrderService--xOrderProcessor: validation failure
        OrderProcessor--xClient: rejected order
    else order is valid
        OrderValidationService-->>-OrderService: validation passed
        OrderService->>OrderService: create order with status NEW
        OrderService->>OrderService: save new order
        OrderService-->>-OrderProcessor: order

        alt side is BUY
            OrderProcessor->>OrderExecutionStrategy: select BuyOrderStrategy
            OrderProcessor->>+OrderExecutionStrategy: execute(order, account, instrument)
            OrderExecutionStrategy->>+AccountService: debit(account, price x quantity)
            AccountService-->>-OrderExecutionStrategy: balance updated
            OrderExecutionStrategy->>+PositionManager: updatePositionAfterBuy(accountId, symbol, quantity, price)
            PositionManager->>PositionRepository: find or create position
            PositionRepository-->>PositionManager: position
            PositionManager->>+PositionService: applyBuy(position, quantity, price)
            PositionService->>PositionService: add quantity and recalculate average cost
            PositionService-->>-PositionManager: updated position
            PositionManager->>PositionRepository: save(position)
            PositionRepository-->>PositionManager: saved
            PositionManager-->>-OrderExecutionStrategy: position updated
        else side is SELL
            OrderProcessor->>OrderExecutionStrategy: select SellOrderStrategy
            OrderProcessor->>+OrderExecutionStrategy: execute(order, account, instrument)
            OrderExecutionStrategy->>+AccountService: credit(account, price x quantity)
            AccountService-->>-OrderExecutionStrategy: balance updated
            OrderExecutionStrategy->>+PositionManager: updatePositionAfterSell(accountId, symbol, quantity)
            PositionManager->>PositionRepository: find position
            PositionRepository-->>PositionManager: current position
            PositionManager->>+PositionService: applySell(position, quantity)
            PositionService->>PositionService: subtract quantity
            PositionService-->>-PositionManager: updated position

            alt position quantity reaches zero
                PositionManager->>PositionRepository: delete(accountId, symbol)
                PositionRepository-->>PositionManager: deleted
            else position quantity remains
                PositionManager->>PositionRepository: save(position)
                PositionRepository-->>PositionManager: saved
            end

            PositionManager-->>-OrderExecutionStrategy: position updated
        end

        OrderExecutionStrategy->>OrderExecutionStrategy: mark order FILLED
        OrderExecutionStrategy-->>-OrderProcessor: OrderResult(success)
        OrderProcessor->>OrderService: saveOrder(order with final status)
        OrderService-->>OrderProcessor: saved order
        OrderProcessor-->>-Client: OrderResult
    end

    Note over OrderExecutionStrategy,PositionRepository: On execution failure, the strategy marks the order REJECTED and attempts to reverse the cash movement.
```
