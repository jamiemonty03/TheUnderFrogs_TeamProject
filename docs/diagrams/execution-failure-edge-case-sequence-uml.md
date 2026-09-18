# Buy Order Execution Failure - UML Sequence Diagram

This edge case shows a `BUY` order that passes validation but fails because an
existing position has a corrupted `null` average cost. The resulting
`NullPointerException` occurs during the position cost calculation. The
`BuyOrderStrategy` reverses the cash debit, marks the order `REJECTED`, and
returns a failed result. `Client` is illustrative because production code does
not currently call this flow.

```mermaid
sequenceDiagram
    participant Client
    participant OrderProcessor
    participant OrderService
    participant BuyOrderStrategy
    participant AccountService
    participant PositionService
    participant PositionRepository

    Client->>OrderProcessor: processOrder(..., BUY, ...)
    OrderProcessor->>OrderService: placeOrder(...)
    OrderService-->>OrderProcessor: validated Order
    OrderProcessor->>BuyOrderStrategy: execute(order, account, instrument)
    BuyOrderStrategy->>AccountService: debit(account, totalCost)
    AccountService-->>BuyOrderStrategy: cash debited

    rect rgb(255, 235, 235)
    BuyOrderStrategy->>PositionService: updatePositionAfterBuy(...)
    PositionService->>PositionRepository: findByAccountAndSymbol(accountId, symbol)
    PositionRepository-->>PositionService: Position(quantity, averageCost = null)
    PositionService->>PositionService: currentAverageCost.multiply(currentQuantity)
    PositionService--xBuyOrderStrategy: NullPointerException
    end

    BuyOrderStrategy->>AccountService: credit(account, totalCost)
    AccountService-->>BuyOrderStrategy: cash restored
    BuyOrderStrategy->>BuyOrderStrategy: set status REJECTED
    BuyOrderStrategy-->>OrderProcessor: OrderResult(false, failure message)
    OrderProcessor->>OrderService: saveOrder(order)
    OrderService-->>Client: rejected order result
```
