CREATE SCHEMA IF NOT EXISTS analytics;

-- etl_watermark: Tracks the high watermark for incremental loads
-- Stores the last successfully processed created_at timestamp from orders
CREATE TABLE IF NOT EXISTS analytics.etl_watermark (
    watermark_id SERIAL PRIMARY KEY,
    high_watermark TIMESTAMP NOT NULL,
    batch_id VARCHAR(50) NOT NULL,
    batch_start_time TIMESTAMP NOT NULL,
    batch_end_time TIMESTAMP NOT NULL,
    row_count_loaded INT DEFAULT 0,
    row_count_dead_lettered INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);
