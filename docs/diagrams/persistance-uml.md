---

# Persistence Layer – UML Class Diagram

The persistence layer follows the **Repository Pattern**, abstracting data access logic behind interfaces. The architecture uses in-memory implementations for integration testing and is designed to support database implementations in the future.

```mermaid
classDiagram
    %% Interface Definitions
    class AccountRepository:::interface {
        <<interface>>
        +save(Account) Account
        +findById(String) Optional~Account~
        +delete(String) boolean
        +exists(String) boolean
    }

    class OrderRepository:::interface {
        <<interface>>
        +save(Order) Order
        +findById(String) Optional~Order~
        +findByAccountId(String) List~Order~
        +delete(String) boolean
        +exists(String) boolean
    }

    class InstrumentRepository:::interface {
        <<interface>>
        +save(Instrument) Instrument
        +findBySymbol(String) Optional~Instrument~
        +delete(String) boolean
        +exists(String) boolean
    }

    class PositionRepository:::interface {
        <<interface>>
        +save(Position) Position
        +findByAccountAndSymbol(String, String) Optional~Position~
        +findByAccountId(String) List~Position~
        +delete(String, String) boolean
        +exists(String, String) boolean
    }

    %% In-Memory Implementations
    class InMemoryAccountRepository:::implementation {
        -Map~String, Account~ accounts
        +save(Account) Account
        +findById(String) Optional~Account~
        +delete(String) boolean
        +exists(String) boolean
    }

    class InMemoryOrderRepository:::implementation {
        -Map~String, Order~ orders
        +save(Order) Order
        +findById(String) Optional~Order~
        +findByAccountId(String) List~Order~
        +delete(String) boolean
        +exists(String) boolean
    }

    class InMemoryInstrumentRepository:::implementation {
        -Map~String, Instrument~ instruments
        +save(Instrument) Instrument
        +findBySymbol(String) Optional~Instrument~
        +delete(String) boolean
        +exists(String) boolean
    }

    class InMemoryPositionRepository:::implementation {
        -Map~String, Position~ positions
        -generateKey(String, String) String
        +save(Position) Position
        +findByAccountAndSymbol(String, String) Optional~Position~
        +findByAccountId(String) List~Position~
        +delete(String, String) boolean
        +exists(String, String) boolean
    }

    %% Domain Models
    class Account:::model {
        -String accountId
        -String holderName
        -BigDecimal cashBalance
        -AccountStatus status
    }

    class Order:::model {
        -String orderId
        -String accountId
        -String symbol
        -OrderSide side
        -int quantity
        -BigDecimal price
        -String idempotencyKey
    }

    class Instrument:::model {
        -String symbol
        -String name
        -String assetClass
        -String currency
        -String exchange
        -boolean tradable
    }

    class Position:::model {
        -String accountId
        -String symbol
        -BigDecimal quantity
        -BigDecimal averageCost
    }

    %% Relationships
    AccountRepository <|.. InMemoryAccountRepository : implements
    OrderRepository <|.. InMemoryOrderRepository : implements
    InstrumentRepository <|.. InMemoryInstrumentRepository : implements
    PositionRepository <|.. InMemoryPositionRepository : implements

    InMemoryAccountRepository --> Account : manages
    InMemoryOrderRepository --> Order : manages
    InMemoryInstrumentRepository --> Instrument : manages
    InMemoryPositionRepository --> Position : manages

    classDef interface fill:#e7f3ff,stroke:#0066cc,stroke-width:2px,color:#000
    classDef implementation fill:#f0f8e0,stroke:#339900,stroke-width:2px,color:#000
    classDef model fill:#fff4e6,stroke:#ff9900,stroke-width:2px,color:#000
```
