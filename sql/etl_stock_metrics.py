import os
import sys
import logging
from contextlib import contextmanager
from datetime import datetime, timezone
from typing import Dict, Any
import argparse
import time

import pandas as pd
import psycopg
from psycopg import sql
from psycopg.errors import Error as PsycopgError
from dotenv import load_dotenv

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

EXTRACT_CLEAN_PRICES = sql.SQL("""
    SELECT 
        cp.symbol,
        cp.date,
        cp.open,
        cp.high,
        cp.low,
        cp.close,
        cp.volume,
        i.name AS ticker
    FROM clean_prices cp
    JOIN instruments i ON cp.symbol = i.symbol
    WHERE cp.date >= CURRENT_DATE - MAKE_INTERVAL(days => %s)
        AND i.tradable = TRUE
    ORDER BY cp.symbol, cp.date
""")

INSERT_PRICE_METRICS = sql.SQL("""
    INSERT INTO price_metrics 
    (ticker, trade_date, close_price, daily_return, 
     moving_avg_20, moving_avg_50, avg_volume_30d,
     volatility_30d, volume_spike_ratio, momentum_score, created_at)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    ON CONFLICT (ticker, trade_date) 
    DO UPDATE SET
        close_price = EXCLUDED.close_price,
        daily_return = EXCLUDED.daily_return,
        moving_avg_20 = EXCLUDED.moving_avg_20,
        moving_avg_50 = EXCLUDED.moving_avg_50,
        avg_volume_30d = EXCLUDED.avg_volume_30d,
        volatility_30d = EXCLUDED.volatility_30d,
        volume_spike_ratio = EXCLUDED.volume_spike_ratio,
        momentum_score = EXCLUDED.momentum_score,
        created_at = EXCLUDED.created_at
""")

DB_HOST = os.getenv('DB_HOST')
DB_PORT = os.getenv('DB_PORT', '5432')
DB_NAME = os.getenv('POSTGRES_DB')
DB_USER = os.getenv('POSTGRES_USER')
DB_PASSWORD = os.getenv('POSTGRES_PASSWORD', '')

if not all([DB_HOST, DB_NAME, DB_USER]):
    logger.error("Missing required environment variables: DB_HOST, POSTGRES_DB, POSTGRES_USER")
    sys.exit(1)


class DatabaseConnection:
    
    def __init__(self, max_retries: int = 3):
        self.host = DB_HOST
        self.port = DB_PORT
        self.dbname = DB_NAME
        self.user = DB_USER
        self.password = DB_PASSWORD
        self.max_retries = max_retries
    
    def _connect_with_retry(self):
        for attempt in range(self.max_retries):
            try:
                conn = psycopg.connect(
                    host=self.host,
                    port=self.port,
                    dbname=self.dbname,
                    user=self.user,
                    password=self.password
                )
                logger.info(f"Connected to {self.dbname} at {self.host}:{self.port}")
                return conn
            except PsycopgError as e:
                if attempt < self.max_retries - 1:
                    wait_time = 2 ** attempt
                    logger.warning(f"Connection attempt {attempt + 1} failed. Retrying in {wait_time}s...")
                    time.sleep(wait_time)
                else:
                    logger.error(f"Failed to connect after {self.max_retries} attempts: {e}")
                    raise
    
    @contextmanager
    def get_connection(self):
        conn = None
        try:
            conn = self._connect_with_retry()
            yield conn
        finally:
            if conn:
                conn.close()
    
    @contextmanager
    def get_cursor(self):
        with self.get_connection() as conn:
            try:
                cursor = conn.cursor()
                yield cursor
            finally:
                cursor.close()


def extract_clean_prices(db: DatabaseConnection, lookback_days: int = 365) -> pd.DataFrame:
    try:
        with db.get_cursor() as cursor:
            cursor.execute(EXTRACT_CLEAN_PRICES, (lookback_days,))
            columns = [desc[0] for desc in cursor.description]
            data = cursor.fetchall()
            df = pd.DataFrame(data, columns=columns)
        
        logger.info(f"Extracted {len(df)} clean price records")
        return df
    
    except PsycopgError as e:
        logger.error(f"Failed to extract clean prices: {e}")
        raise


def calculate_daily_return(group: pd.DataFrame) -> pd.Series:
    return group['close'].pct_change()


def calculate_moving_average(group: pd.DataFrame, window: int) -> pd.Series:
    return group['close'].rolling(window=window, min_periods=1).mean()


def calculate_volatility(group: pd.DataFrame, window: int = 30) -> pd.Series:
    returns = group['close'].pct_change()
    volatility = returns.rolling(window=window, min_periods=1).std()
    return volatility


def calculate_volume_metrics(group: pd.DataFrame, window: int = 30) -> tuple:
    avg_volume = group['volume'].rolling(window=window, min_periods=1).mean()
    volume_spike_ratio = group['volume'] / avg_volume
    return avg_volume, volume_spike_ratio


def calculate_momentum_score(
    returns: pd.Series, 
    volatility: pd.Series, 
    price_trend: pd.Series,
    window: int = 20
) -> pd.Series:
    cumulative_returns = returns.rolling(window=window, min_periods=1).sum()
    
    momentum = cumulative_returns * 100
    momentum = momentum.clip(-100, 100)
    
    volatility_penalty = volatility.fillna(0) * 50
    momentum = momentum - volatility_penalty
    
    sma_short = price_trend.rolling(window=5, min_periods=1).mean()
    sma_long = price_trend.rolling(window=20, min_periods=1).mean()
    trend_boost = pd.Series(0, index=momentum.index)
    trend_boost[sma_short > sma_long] = 10
    momentum = momentum + trend_boost
    
    momentum = momentum.clip(-100, 100)
    
    return momentum


def transform_clean_to_metrics(df: pd.DataFrame) -> pd.DataFrame:
    df['date'] = pd.to_datetime(df['date'])
    
    df['close'] = pd.to_numeric(df['close'], errors='coerce')
    df['open'] = pd.to_numeric(df['open'], errors='coerce')
    df['high'] = pd.to_numeric(df['high'], errors='coerce')
    df['low'] = pd.to_numeric(df['low'], errors='coerce')
    df['volume'] = pd.to_numeric(df['volume'], errors='coerce')
    
    null_counts = df[['close', 'open', 'high', 'low', 'volume']].isnull().sum()
    if null_counts.sum() > 0:
        logger.warning(f"Data conversion resulted in NaN values: {null_counts[null_counts > 0].to_dict()}")
    
    df = df.sort_values(['symbol', 'date']).reset_index(drop=True)
    
    metrics_data = []
    
    for symbol, group in df.groupby('symbol'):
        group = group.sort_values('date').reset_index(drop=True)
        
        metrics = pd.DataFrame({
            'symbol': group['symbol'],
            'ticker': group['ticker'],
            'trade_date': group['date'],
            'close_price': group['close'],
            'volume': group['volume'],
        })
        
        metrics['daily_return'] = calculate_daily_return(group)
        
        metrics['moving_avg_20'] = calculate_moving_average(group, 20)
        metrics['moving_avg_50'] = calculate_moving_average(group, 50)
        
        avg_volume, volume_spike_ratio = calculate_volume_metrics(group, 30)
        metrics['avg_volume_30d'] = avg_volume
        metrics['volume_spike_ratio'] = volume_spike_ratio
        
        metrics['volatility_30d'] = calculate_volatility(group, 30)
        
        metrics['momentum_score'] = calculate_momentum_score(
            metrics['daily_return'],
            metrics['volatility_30d'],
            group['close']
        )
        
        metrics['created_at'] = datetime.now(timezone.utc)
        
        metrics_data.append(metrics)
    
    result_df = pd.concat(metrics_data, ignore_index=True)
    logger.info(f"Transformed into {len(result_df)} metric records")
    
    return result_df


def validate_metrics(df: pd.DataFrame) -> bool:
    required_columns = [
        'ticker', 'trade_date', 'close_price', 'daily_return',
        'moving_avg_20', 'moving_avg_50', 'avg_volume_30d',
        'volatility_30d', 'volume_spike_ratio', 'momentum_score', 'created_at'
    ]
    
    missing_cols = set(required_columns) - set(df.columns)
    if missing_cols:
        raise ValueError(f"Missing required columns: {missing_cols}")
    
    critical_cols = ['ticker', 'trade_date', 'close_price']
    null_in_critical = df[critical_cols].isnull().sum()
    if null_in_critical.sum() > 0:
        raise ValueError(f"Null values found in critical columns:\n{null_in_critical}")
    
    if not pd.api.types.is_numeric_dtype(df['close_price']):
        raise ValueError("close_price must be numeric")
    
    if not pd.api.types.is_numeric_dtype(df['momentum_score']):
        raise ValueError("momentum_score must be numeric")
    
    if (df['close_price'] <= 0).any():
        raise ValueError("close_price must be greater than 0")
    
    if ((df['momentum_score'] < -100) | (df['momentum_score'] > 100)).any():
        raise ValueError("momentum_score must be between -100 and 100")
    
    if (df['volume'] < 0).any():
        raise ValueError("volume cannot be negative")
    
    logger.info("Validation passed")
    return True


def load_to_database(db: DatabaseConnection, df: pd.DataFrame) -> int:
    try:
        # Read and execute table creation SQL
        sql_file_path = os.path.join(os.path.dirname(__file__), 'tables', '07-price_metrics.sql')
        with open(sql_file_path, 'r') as f:
            sql_content = f.read()
        
        with db.get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(sql_content)
                conn.commit()
            
            logger.info("price_metrics table ready")
            
            records_loaded = 0
            with conn.cursor() as cursor:
                batches = [df.iloc[i:i+1000] for i in range(0, len(df), 1000)]
                for batch_idx, batch in enumerate(batches):
                    batch_data = [
                        (
                            row['ticker'],
                            row['trade_date'],
                            float(row['close_price']),
                            float(row['daily_return']) if pd.notna(row['daily_return']) else None,
                            float(row['moving_avg_20']) if pd.notna(row['moving_avg_20']) else None,
                            float(row['moving_avg_50']) if pd.notna(row['moving_avg_50']) else None,
                            float(row['avg_volume_30d']) if pd.notna(row['avg_volume_30d']) else None,
                            float(row['volatility_30d']) if pd.notna(row['volatility_30d']) else None,
                            float(row['volume_spike_ratio']) if pd.notna(row['volume_spike_ratio']) else None,
                            float(row['momentum_score']) if pd.notna(row['momentum_score']) else None,
                            row['created_at']
                        )
                        for _, row in batch.iterrows()
                    ]
                    
                    cursor.executemany(INSERT_PRICE_METRICS, batch_data)
                    records_loaded += len(batch_data)
                    logger.info(f"Batch {batch_idx + 1}: inserted {len(batch_data)} records")
                
                conn.commit()
            
            logger.info(f"Successfully loaded {records_loaded} records to price_metrics")
            return records_loaded
    
    except PsycopgError as e:
        logger.error(f"Failed to load data to database: {e}")
        raise


def run_etl_pipeline(lookback_days: int = 365) -> Dict[str, Any]:
    start_time = datetime.now(timezone.utc)
    
    try:
        db = DatabaseConnection()
        logger.info("=" * 80)
        logger.info("Starting ETL Pipeline for Stock Price Metrics")
        logger.info("=" * 80)
        
        clean_df = extract_clean_prices(db, lookback_days)
        
        if clean_df.empty:
            logger.warning("No clean price data found")
            return {
                'status': 'warning',
                'message': 'No data to process',
                'records_processed': 0,
                'duration_seconds': (datetime.now(timezone.utc) - start_time).total_seconds()
            }
        
        metrics_df = transform_clean_to_metrics(clean_df)
        
        validate_metrics(metrics_df)
        
        records_loaded = load_to_database(db, metrics_df)
        
        end_time = datetime.now(timezone.utc)
        duration = (end_time - start_time).total_seconds()
        
        summary = {
            'status': 'success',
            'start_time': start_time.isoformat(),
            'end_time': end_time.isoformat(),
            'duration_seconds': duration,
            'records_extracted': len(clean_df),
            'records_loaded': records_loaded,
            'unique_symbols': metrics_df['symbol'].nunique()
        }
        
        logger.info("=" * 80)
        logger.info("ETL Pipeline Completed Successfully")
        logger.info(f"Records extracted: {summary['records_extracted']}")
        logger.info(f"Records loaded: {summary['records_loaded']}")
        logger.info(f"Duration: {duration:.2f} seconds")
        logger.info("=" * 80)
        
        return summary
    
    except Exception as e:
        end_time = datetime.now(timezone.utc)
        duration = (end_time - start_time).total_seconds()
        
        logger.error("=" * 80)
        logger.error(f"ETL Pipeline Failed: {str(e)}")
        logger.error("=" * 80)
        
        return {
            'status': 'error',
            'error': str(e),
            'duration_seconds': duration
        }


def main():
    parser = argparse.ArgumentParser(description='ETL pipeline for stock price metrics')
    parser.add_argument('--lookback', type=int, default=365, 
                        help='Number of days of historical data to process (default: 365)')
    args = parser.parse_args()
    
    result = run_etl_pipeline(args.lookback)
    
    if result['status'] == 'success':
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
