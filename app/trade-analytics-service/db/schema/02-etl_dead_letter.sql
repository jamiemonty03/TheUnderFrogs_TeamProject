CREATE SCHEMA IF NOT EXISTS analytics;

-- etl_dead_letter: Audit table for rows that fail quality validation
-- Captures invalid rows with context for investigation and replay
CREATE TABLE IF NOT EXISTS analytics.etl_dead_letter (
    dead_letter_id SERIAL PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL,
    source_table VARCHAR(50) NOT NULL,
    source_row JSONB NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dead_letter_batch_id ON analytics.etl_dead_letter(batch_id);
CREATE INDEX IF NOT EXISTS idx_dead_letter_source_table ON analytics.etl_dead_letter(source_table);
