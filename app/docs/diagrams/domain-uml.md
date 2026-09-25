# Domain Model – UML Class Diagram

No model class here holds a direct object reference to another — all
relationships are joined by matching ID fields (`accountId`, `symbol`), not
Java fields. Solid line (`Order → Instrument`) is enforced in both the DB and
in Java (`OrderService`/`OrderValidationService`). Dashed lines
(`Position`/`Price`/`Asset` → `Instrument`) are enforced by a DB foreign key
only — nothing in the Java code checks them yet.

```mermaid
classDiagram
    class Instrument:::reference {
        -String symbol
        -String name
        -String assetClass
        -String currency
        -String exchange
        -boolean tradable
        -int version
        -LocalDateTime createdAt
        -LocalDateTime lastUpdated
        -String updatedBy
        +isTradable() boolean
    }

    class Asset:::assets {
        <<abstract>>
        -String symbol
        -String name
        -BigDecimal price
        -LocalDateTime tradeDate
        -int version
        -LocalDateTime createdAt
        -LocalDateTime lastUpdated
        -String updatedBy
    }

    class Stock:::assets {
        -String sector
        -String industry
        -String country
        -Integer fullTimeEmployees
        -BigDecimal beta
        -BigDecimal trailingPe
        -BigDecimal forwardPe
        -BigDecimal trailingEps
        -BigDecimal dividendRate
        -BigDecimal payoutRatio
        -BigDecimal priceToBook
        -BigDecimal returnOnEquity
        -BigDecimal marketCap
        -BigDecimal sharesOutstanding
        -BigDecimal totalRevenue
        -String website
    }

    class Bond:::assets {
        -String category
        -String fundFamily
        -String legalType
        -BigDecimal netExpenseRatio
        -BigDecimal navPrice
        -BigDecimal totalAssets
        -BigDecimal netAssets
        -BigDecimal yieldToMaturity
        -BigDecimal couponRate
        -BigDecimal duration
        -BigDecimal ytdReturn
        -BigDecimal threeYearAvgReturn
        -BigDecimal fiveYearAvgReturn
        -BigDecimal beta3Year
        -BigDecimal distributionYield
    }

    class Etf:::assets {
        -String category
        -String fundFamily
        -String legalType
        -BigDecimal netExpenseRatio
        -BigDecimal navPrice
        -BigDecimal totalAssets
        -BigDecimal netAssets
        -BigDecimal ytdReturn
        -BigDecimal threeYearAvgReturn
        -BigDecimal fiveYearAvgReturn
        -BigDecimal beta3Year
        -BigDecimal distributionYield
    }

    class Price:::reference {
        -String symbol
        -LocalDate tradeDate
        -BigDecimal open
        -BigDecimal high
        -BigDecimal low
        -BigDecimal close
        -BigDecimal volume
        -LocalDateTime createdAt
    }

    class Account:::account {
        -String accountId
        -String holderName
        -BigDecimal cashBalance
        -AccountStatus status
        -int version
        -LocalDateTime createdAt
        -LocalDateTime lastUpdated
        -String updatedBy
        +isActive() boolean
    }

    class Order:::order {
        -String orderId
        -String accountId
        -String symbol
        -OrderSide side
        -int quantity
        -BigDecimal price
        -String idempotencyKey
        -OrderStatus orderStatus
        -int version
        -LocalDateTime createdAt
        -LocalDateTime lastUpdated
        -String updatedBy
    }

    class Position:::joinEntity {
        -String accountId
        -String symbol
        -BigDecimal quantity
        -BigDecimal averageCost
        -int version
        -LocalDateTime createdAt
        -LocalDateTime lastUpdated
        -String updatedBy
        +getTotalCostBasis() BigDecimal
        +getMarketValue(BigDecimal currentPrice) BigDecimal
    }

    class AccountStatus:::account {
        <<enumeration>>
        ACTIVE
        CLOSED
        SUSPENDED
    }

    class OrderSide:::order {
        <<enumeration>>
        BUY
        SELL
    }

    class OrderStatus:::order {
        <<enumeration>>
        NEW
        FILLED
        REJECTED
        CANCELLED
    }

    Asset <|-- Stock
    Asset <|-- Bond
    Asset <|-- Etf

    Account "1" -- "0..*" Order : accountId (logical FK)
    Account "1" -- "0..*" Position : accountId (logical FK)
    Account "1" -- "1" AccountStatus : has

    Order "0..*" --> "1" Instrument : used by OrderService

    Position "0..*" ..> "1" Instrument : symbol (DB FK)
    Price "0..*" ..> "1" Instrument : symbol (DB FK)
    Asset "0..*" ..> "1" Instrument : symbol (DB FK)

    Order "1" -- "1" OrderSide : has
    Order "1" -- "1" OrderStatus : has

    classDef account fill:#cfe2ff,stroke:#0d6efd,color:#000
    classDef order fill:#e2d9f3,stroke:#6f42c1,color:#000
    classDef assets fill:#d4edda,stroke:#198754,color:#000
    classDef reference fill:#fff3cd,stroke:#fd7e14,color:#000
    classDef joinEntity fill:#e2e3e5,stroke:#6c757d,color:#000
```
