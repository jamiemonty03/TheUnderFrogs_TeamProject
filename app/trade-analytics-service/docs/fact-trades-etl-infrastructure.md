# ETL Infrastructure: Watermark & Dead-Letter Schema

This diagram shows how the **etl_watermark** and **etl_dead_letter** tables interact with the FACT_TRADES star schema to enable incremental loading with full audit trail.

## ER Diagram: Data Relationships

```mermaid
erDiagram
    etl_watermark ||--o{ FACT_TRADES : "enables_incremental_load"
    etl_watermark ||--o{ etl_dead_letter : "batch_id"
    etl_dead_letter ||--|{ FACT_TRADES : "source_table=orders"

    etl_watermark {
        string watermark_id PK "UUID or auto-increment"
        timestamp high_watermark "Last processed order created_at"
        string batch_id FK "ISO timestamp of ETL run"
        timestamp batch_start_time "When ETL started"
        timestamp batch_end_time "When ETL finished"
        int row_count_loaded "Number of rows in FACT_TRADES"
        int row_count_dead_lettered "Number of rows in etl_dead_letter"
        timestamp created_at "Record creation time"
    }

    etl_dead_letter {
        int dead_letter_id PK "Auto-increment ID"
        string batch_id FK "Links to watermark.batch_id"
        string source_table "orders (source DB table name)"
        json source_row "Full original row that failed"
        string reason "Validation failure reason"
        timestamp created_at "When it was dead-lettered"
    }

    FACT_TRADES {
        string trade_key PK "order_id"
        int account_key FK "→ DIM_ACCOUNT"
        int instrument_key FK "→ DIM_INSTRUMENT"
        int date_key FK "→ DIM_DATE"
        string side "BUY or SELL"
        int quantity "Order quantity"
        numeric price "Order price"
        string order_status "NEW, FILLED, REJECTED, CANCELLED"
        timestamp last_updated "When fact row was created or updated"
    }
```

---

## ETL Process Flow: Watermark & Dead-Letter Lifecycle

```mermaid
graph TD
    A["ETL Run Started<br/>batch_id = 2026-09-29T14:32:00Z"] -->|Read Watermark| B["Query etl_watermark<br/>high_watermark = 2026-09-29T10:00:00Z"]
    
    B -->|Extract Orders| C["SELECT * FROM orders_db.orders<br/>WHERE created_at > high_watermark<br/>UNION<br/>Re-process orders still NEW"]
    
    C -->|Validate Each Order| D{Quality Check<br/>qty>0, price>0<br/>account exists<br/>instrument exists?}
    
    D -->|✓ Valid| E["Upsert Dimensions<br/>DIM_ACCOUNT<br/>DIM_INSTRUMENT<br/>DIM_DATE"]
    
    D -->|✗ Invalid| F["INSERT INTO etl_dead_letter<br/>batch_id = 2026-09-29T14:32:00Z<br/>reason = validation failure<br/>source_row = original order JSON"]
    
    E -->|All Dimensions Ready| G["MERGE INTO FACT_TRADES<br/>ON trade_key = order_id<br/>UPDATE if exists<br/>INSERT if new"]
    
    F -->|Failures Recorded| H["Accumulate<br/>row_count_dead_lettered"]
    
    G -->|Facts Loaded| I["Accumulate<br/>row_count_loaded"]
    
    H --> J["Atomic Commit"]
    I --> J
    
    J -->|Transaction Success| K["INSERT INTO etl_watermark<br/>high_watermark = MAX created_at<br/>batch_id = 2026-09-29T14:32:00Z<br/>row_count_loaded = N<br/>row_count_dead_lettered = M"]
    
    J -->|Transaction Failure| L["ROLLBACK<br/>No watermark update<br/>Next run retries same data"]
    
    K -->|Next ETL Run| B
    L -->|Retry| B
```

---

## How Watermark Enables Incremental Loading

### Run 1 (First Time)
```
Watermark Table (empty initially)
→ Initialize to NOW() - 90 days
→ Extract orders created after that date
→ Load into FACT_TRADES
→ Update watermark: high_watermark = MAX(order.created_at) = 2026-09-28 15:00:00
```

### Run 2 (Next Day)
```
Read watermark: 2026-09-28 15:00:00
→ Extract ONLY orders created AFTER 2026-09-28 15:00:00
→ PLUS re-process orders still in NEW status (to catch status changes)
→ Update watermark: high_watermark = MAX(order.created_at) = 2026-09-29 14:30:00
```

### Run 3 (If Run 2 Failed)
```
Read watermark: 2026-09-28 15:00:00  (not updated because Run 2 rolled back)
→ Extract same orders as Run 2
→ Deduplicated by MERGE ON trade_key (no double-counting)
→ Update watermark: high_watermark = 2026-09-29 14:30:00
```

---

## How Dead-Letter Tables Audit Failures

### Scenario: Invalid Order

```
Order arrives in batch_id = 2026-09-29T14:32:00Z:
  order_id: ord-123
  account_id: ACC9999 (doesn't exist in accounts_db)
  quantity: 50
  price: 100.00

Validation fails:
  reason: "account not found: ACC9999"

Instead of silently skipping:
  INSERT INTO etl_dead_letter (
    batch_id,       → '2026-09-29T14:32:00Z'
    source_table,   → 'orders'
    source_row,     → {"order_id":"ord-123","account_id":"ACC9999",...}
    reason,         → 'account not found: ACC9999'
    created_at      → NOW()
  )

Result:
  - FACT_TRADES: NO change (row not inserted)
  - etl_dead_letter: row recorded for audit/replay
  - batch stats: row_count_dead_lettered incremented
  - Analytics team can query and investigate
```

---

## Batch Atomicity: All-or-Nothing

The entire ETL run is **one transaction**:

```sql
BEGIN TRANSACTION;

  -- Upsert all dimensions
  INSERT INTO analytics.DIM_ACCOUNT ... ON CONFLICT ... ;
  INSERT INTO analytics.DIM_INSTRUMENT ... ON CONFLICT ... ;
  INSERT INTO analytics.DIM_DATE ... ON CONFLICT ... ;
  
  -- Record dead-letters
  INSERT INTO analytics.etl_dead_letter (...) VALUES (...); -- for each invalid row
  
  -- Merge facts
  INSERT INTO analytics.FACT_TRADES ... ON CONFLICT ... ;
  
  -- Update watermark LAST (so it only advances if all above succeed)
  INSERT INTO analytics.etl_watermark (...) VALUES (...);

COMMIT;  -- All succeed together
-- OR ROLLBACK; -- All fail together (if any step errors)
```

**Why**: If the ETL crashes mid-run:
- ✅ Watermark doesn't advance
- ✅ Next run re-processes same orders
- ✅ MERGE ON trade_key deduplicates (no double-counting)
- ✅ All data consistent

---

## Query Patterns: Using Watermark & Dead-Letter

### Monitor ETL Runs
```sql
SELECT 
  batch_id,
  high_watermark,
  row_count_loaded,
  row_count_dead_lettered,
  batch_end_time - batch_start_time as duration
FROM analytics.etl_watermark
ORDER BY created_at DESC
LIMIT 10;
```

### Check Validation Failures
```sql
SELECT 
  batch_id,
  source_table,
  reason,
  COUNT(*) as failure_count
FROM analytics.etl_dead_letter
WHERE created_at > NOW() - INTERVAL '7 days'
GROUP BY batch_id, reason
ORDER BY failure_count DESC;
```

### Investigate Specific Failed Order
```sql
SELECT 
  dead_letter_id,
  source_row,
  reason,
  created_at
FROM analytics.etl_dead_letter
WHERE source_row ->> 'order_id' = 'ord-123';
```

### Verify Idempotency (Watermark Prevents Double-Counting)
```sql
-- Run ETL once: expect 100 trades
SELECT COUNT(*) FROM analytics.FACT_TRADES;
-- Output: 100

-- Manually re-run ETL with same watermark
-- (simulating a retry or re-trigger)
SELECT COUNT(*) FROM analytics.FACT_TRADES;
-- Output: 100 (unchanged, because watermark didn't advance)
```

---

## Integration with Star Schema

The watermark and dead-letter tables **support** the star schema operations:

| Table | Role | Purpose |
|-------|------|---------|
| `etl_watermark` | **Control** | Tracks which orders have been processed; enables next run to load only new data |
| `etl_dead_letter` | **Audit** | Records validation failures with full context (original row, reason, batch) |
| `FACT_TRADES` | **Analytics** | The actual trade events (fact rows with measures & dimensions) |
| `DIM_ACCOUNT`, `DIM_INSTRUMENT`, `DIM_DATE` | **Context** | Reference data for slicing and grouping facts |

---

## Acceptance Criteria: Infrastructure Validation

| Criterion | How It's Validated |
|-----------|-------------------|
| Watermark prevents re-loading old orders | `test_watermark_update_recorded`, `test_idempotency` |
| Dead-letter captures all validation failures | `test_invalid_quantity_zero`, `test_invalid_account_not_found`, etc. |
| Batch atomicity (all-or-nothing commit) | `test_commit_on_success`, `test_rollback_on_error` |
| Batch ID links watermark & dead-letter | `test_dead_letter_contains_full_row_as_json` includes batch_id |
| Order status changes detected & updated | `test_merge_facts_update_existing_row` (re-process NEW orders) |
| Connection retries don't lose data | `test_connection_retry_with_exponential_backoff` → no duplicate on retry |






