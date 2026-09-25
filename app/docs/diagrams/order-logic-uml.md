# Order Processing Logic – UML Class Diagram

This focused diagram shows the business classes involved in processing an order.
Persistence repositories, database implementations, and detailed model fields are
covered in the separate domain and persistence diagrams.

```mermaid
classDiagram
    class OrderProcessor:::orchestration {
        +processOrder(...) OrderResult
    }

    class OrderService:::orderService {
        +placeOrder(...) Order
        +saveOrder(Order) Order
    }

    class OrderValidationService:::validation {
        +validateOrder(...) 
    }

    class OrderExecutionStrategy:::strategy {
        <<interface>>
        +execute(Order, Account, Instrument) OrderResult
        BUY option: debit cash, increase position
        SELL option: credit cash, decrease position
    }

    class AccountService:::accountLogic {
        +credit(Account, BigDecimal)
        +debit(Account, BigDecimal)
    }

    class PositionService:::positionLogic {
        +updatePositionAfterBuy(...)
        +updatePositionAfterSell(...)
        +applyBuy(...)
        +applySell(...)
    }

    class OrderResult:::result {
        +isSuccess() boolean
        +getMessage() String
    }

    class Order:::order {
        +OrderStatus status
        +OrderSide side
    }

    class Account:::account {
        +BigDecimal cashBalance
    }

    class Instrument:::reference {
        +isTradable() boolean
    }

    class Position:::position {
        +BigDecimal quantity
        +BigDecimal averageCost
    }

    OrderProcessor --> OrderService : creates and saves orders
    OrderProcessor --> OrderExecutionStrategy : selects BUY or SELL behavior
    OrderProcessor --> OrderResult : returns

    OrderService --> OrderValidationService : validates through
    OrderService --> Order : creates

    OrderValidationService --> Account : validates funds and status
    OrderValidationService --> Instrument : validates tradability

    OrderExecutionStrategy --> AccountService : changes cash
    OrderExecutionStrategy --> PositionService : changes position
    OrderExecutionStrategy --> OrderResult : returns

    AccountService --> Account : updates cash balance

    PositionService --> Position : changes quantity and cost

    Order --> Account : belongs to
    Order --> Instrument : trades

    classDef orchestration fill:#f8d7da,stroke:#dc3545,color:#000
    classDef orderService fill:#e2d9f3,stroke:#6f42c1,color:#000
    classDef validation fill:#fff3cd,stroke:#fd7e14,color:#000
    classDef strategy fill:#d4edda,stroke:#198754,color:#000
    classDef accountLogic fill:#cfe2ff,stroke:#0d6efd,color:#000
    classDef positionLogic fill:#cfe2ff,stroke:#0d6efd,color:#000
    classDef positionCalculation fill:#d1ecf1,stroke:#17a2b8,color:#000
    classDef result fill:#e2e3e5,stroke:#6c757d,color:#000
    classDef order fill:#e2d9f3,stroke:#6f42c1,color:#000
    classDef account fill:#cfe2ff,stroke:#0d6efd,color:#000
    classDef reference fill:#fff3cd,stroke:#fd7e14,color:#000
    classDef position fill:#e2e3e5,stroke:#6c757d,color:#000
```
