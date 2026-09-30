# FACT_TRADES Star Schema – ER Diagram

This diagram visualizes the dimensional data warehouse schema for trade analytics. The central FACT_TRADES table connects to three dimensions (Account, Instrument, Date) enabling efficient aggregation and slicing by account, instrument, and time

```mermaid
erDiagram
    FACT_TRADES ||--o{ DIM_ACCOUNT : "account_key FK"
    FACT_TRADES ||--o{ DIM_INSTRUMENT : "instrument_key FK"
    FACT_TRADES ||--o{ DIM_DATE : "date_key FK"

    FACT_TRADES {
        string trade_key PK "order_id (UUID)"
        int account_key FK "→ DIM_ACCOUNT"
        int instrument_key FK "→ DIM_INSTRUMENT"
        int date_key FK "→ DIM_DATE"
        string side "BUY or SELL"
        int quantity "Order quantity"
        numeric price "Order price (18,2)"
        string order_status "NEW, FILLED, REJECTED, CANCELLED"
        timestamp last_updated "Update timestamp"
    }

    DIM_ACCOUNT {
        int account_key PK "Surrogate key"
        string account_id "Account ID (business key)"
        string holder_name "Account holder name"
        numeric cash_balance "Cash balance (18,2)"
        string account_status "ACTIVE, CLOSED, SUSPENDED"
        int user_id "Foreign key to users"
        timestamp last_updated "Update timestamp"
    }

    DIM_INSTRUMENT {
        int instrument_key PK "Surrogate key"
        string symbol "Stock ticker (business key)"
        string name "Instrument name"
        string asset_class "Equity, ETF, or Bond"
        string currency "ISO 4217 code"
        string exchange "Exchange name"
        boolean tradable "Is currently tradable"
        int version "SCD Type 2 version"
        timestamp last_updated "Update timestamp"
    }

    DIM_DATE {
        int date_key PK "YYYYMMDD format"
        date date_value "Actual date"
        int year "Year (e.g., 2026)"
        int quarter "Quarter (1-4)"
        int month "Month (1-12)"
        int day "Day of month"
        int day_of_week "1=Monday, 7=Sunday"
        int week_of_year "ISO week"
        string day_name "Monday, Tuesday, etc."
        string month_name "January, February, etc."
        boolean is_weekend "Saturday or Sunday"
    }
```

---

## Meaningful Analysis: What This Schema Enables

This data can be **joined and sliced** to provide meaningful insights into:

- **Instrument Analysis**: Which asset classes (Equity/ETF/Bond) drive the most volume? Which symbols are most traded?
- **Account Profitability**: Which accounts generate the highest transaction values? Who are the most active traders?
- **Buy-Sell Ratios**: Do certain accounts prefer buying over selling? Which instruments see more sell pressure?
- **Average Price Points**: Are certain accounts trading micro-caps vs blue chips? How does price correlate with volume?
- **Trending Patterns**: Seasonal trends (Q3 vs Q4), day-of-week effects (Mondays vs Fridays), weekend vs weekday trading
- **Order Status Flow**: What % of orders move from NEW → FILLED? How many are REJECTED or CANCELLED?
- **Cash Flow Correlation**: How does account cash_balance correlate with trading frequency?
- **Risk & Anomalies**: Spot accounts with unusual trading patterns, instrument concentration risk, dead-lettered validation failures