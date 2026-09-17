# Account Management – UML Sequence Diagram

Comprehensive sequence diagram tracing `AccountService.credit()` and `AccountService.debit()` operations. Shows validation checkpoints, state updates, and exception paths for account cash balance management.

The flow validates in strict sequence — validation stops immediately on first failure. Only successful validation leads to balance update and version increment.

## Credit Operation Flow
```mermaid
sequenceDiagram
    participant Client
    participant AccountService
    participant Account

    Client->>AccountService: credit(account, amount)
    
    Note over AccountService: 1. Validate amount
    rect rgb(255, 235, 235)
    alt amount is null
        AccountService--xClient: NullPointerException
    else amount <= 0
        AccountService--xClient: IllegalArgumentException
    end
    end

    Note over AccountService: 2. Check account is active
    rect rgb(255, 235, 235)
    alt account is null
        AccountService--xClient: NullPointerException
    else account status != ACTIVE
        AccountService--xClient: AccountNotActiveException
    end
    end

    Note over AccountService: 3. Update balance and version
    rect rgb(235, 245, 255)
    AccountService->>Account: setCashBalance(currentBalance + amount)
    AccountService->>Account: setVersion(version + 1)
    end

    rect rgb(235, 255, 235)
    AccountService-->>Client: Account returned (updated)
    end
    Note over AccountService,Client: Balance increased, version incremented
```

## Debit Operation Flow
```mermaid
sequenceDiagram
    participant Client
    participant AccountService
    participant Account

    Client->>AccountService: debit(account, amount)
    
    Note over AccountService: 1. Validate amount
    rect rgb(255, 235, 235)
    alt amount is null
        AccountService--xClient: NullPointerException
    else amount <= 0
        AccountService--xClient: IllegalArgumentException
    end
    end

    Note over AccountService: 2. Check account is active
    rect rgb(255, 235, 235)
    alt account is null
        AccountService--xClient: NullPointerException
    else account status != ACTIVE
        AccountService--xClient: AccountNotActiveException
    end
    end

    Note over AccountService: 3. Validate sufficient balance
    rect rgb(255, 235, 235)
    alt amount > cashBalance
        AccountService--xClient: InsufficientFundsException
    end
    end

    Note over AccountService: 4. Update balance and version
    rect rgb(235, 245, 255)
    AccountService->>Account: setCashBalance(currentBalance - amount)
    AccountService->>Account: setVersion(version + 1)
    end

    rect rgb(235, 255, 235)
    AccountService-->>Client: Account returned (updated)
    end
    Note over AccountService,Client: Balance decreased, version incremented
```
