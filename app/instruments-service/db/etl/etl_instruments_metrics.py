import os
from urllib.parse import urlparse
import sys
import logging
from contextlib import contextmanager
from datetime import datetime, timezone, timedelta
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

# SQL Queries
EXTRACT_PRICE_DATA = sql.SQL("""
    SELECT 
        cp.symbol,
        cp.date,
        cp.close,
        i.asset_class,
        i.currency,
        i.exchange
    FROM clean_prices cp
    JOIN instruments i ON cp.symbol = i.symbol
    WHERE cp.date >= CURRENT_DATE - INTERVAL '365 days'
    ORDER BY cp.symbol, cp.date
""")

INSERT_INSTRUMENTS_METRICS = sql.SQL("""
    INSERT INTO instruments_metrics 
    (symbol, latest_price, latest_date, price_52w_high, price_52w_low,
     ytd_return, one_year_return, max_drawdown, last_price_update, 
     days_since_update, asset_class, currency, exchange, version, created_at, last_updated, updated_by)
    VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
    ON CONFLICT (symbol)
    DO UPDATE SET
        latest_price = EXCLUDED.latest_price,
        latest_date = EXCLUDED.latest_date,
        price_52w_high = EXCLUDED.price_52w_high,
        price_52w_low = EXCLUDED.price_52w_low,
        ytd_return = EXCLUDED.ytd_return,
        one_year_return = EXCLUDED.one_year_return,
        max_drawdown = EXCLUDED.max_drawdown,
        last_price_update = EXCLUDED.last_price_update,
        days_since_update = EXCLUDED.days_since_update,
        asset_class = EXCLUDED.asset_class,
        currency = EXCLUDED.currency,
        exchange = EXCLUDED.exchange,
        version = instruments_metrics.version + 1,
        last_updated = NOW(),
        updated_by = 'ETL_PROCESS'
""")

_datasource = urlparse(os.getenv('SPRING_DATASOURCE_URL', '').removeprefix('jdbc:'))
DB_HOST = _datasource.hostname
DB_PORT = str(_datasource.port or 5432)
DB_NAME = _datasource.path.lstrip('/')
DB_USER = os.getenv('SPRING_DATASOURCE_USERNAME')
DB_PASSWORD = os.getenv('SPRING_DATASOURCE_PASSWORD', '')

if not all([DB_HOST, DB_NAME, DB_USER]):
    logger.error("Missing required environment variables: SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME")
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


def extract_price_data(db: DatabaseConnection) -> pd.DataFrame:
    """Extract price data for last 365 days joined with instruments."""
    try:
        with db.get_cursor() as cursor:
            cursor.execute(EXTRACT_PRICE_DATA)
            columns = [desc[0] for desc in cursor.description]
            data = cursor.fetchall()
            df = pd.DataFrame(data, columns=columns)
        
        logger.info(f"Extracted {len(df)} price records")
        return df
    
    except PsycopgError as e:
        logger.error(f"Failed to extract price data: {e}")
        raise


def calculate_metrics(df: pd.DataFrame) -> pd.DataFrame:
    """Calculate all metrics for each symbol."""
    df['date'] = pd.to_datetime(df['date'])
    df['close'] = pd.to_numeric(df['close'], errors='coerce')
    
    # Drop rows with null prices
    df = df.dropna(subset=['close'])
    
    if df.empty:
        logger.warning("No valid price data after cleaning")
        return pd.DataFrame()
    
    # Get today's date
    today = datetime.now(timezone.utc).date()
    ytd_start = pd.Timestamp(year=today.year, month=1, day=1)
    one_year_ago = pd.Timestamp(today - timedelta(days=365))
    
    metrics_list = []
    
    for symbol, group in df.groupby('symbol'):
        group = group.sort_values('date').reset_index(drop=True)
        
        if group.empty:
            continue
        
        # Latest price
        latest_row = group.iloc[-1]
        latest_price = latest_row['close']
        latest_date = latest_row['date'].date()
        
        # 52-week high/low
        price_52w_high = group['close'].max()
        price_52w_low = group['close'].min()
        
        # Year-to-date return
        ytd_data = group[group['date'] >= ytd_start]
        if not ytd_data.empty:
            ytd_return = ((latest_price / ytd_data.iloc[0]['close']) - 1) * 100
        else:
            ytd_return = None
        
        # One-year return
        one_year_data = group[group['date'] >= one_year_ago]
        if not one_year_data.empty:
            one_year_return = ((latest_price / one_year_data.iloc[0]['close']) - 1) * 100
        else:
            one_year_return = None
        
        # Max drawdown (largest peak-to-trough decline)
        cumulative_max = group['close'].expanding().max()
        drawdown = (group['close'] - cumulative_max) / cumulative_max
        max_drawdown = drawdown.min() * 100  # Convert to percentage
        
        # Days since update
        days_since = (today - latest_date).days
        
        # Get instrument info from first row (should be same for all rows of symbol)
        asset_class = latest_row['asset_class']
        currency = latest_row['currency']
        exchange = latest_row['exchange']
        
        metrics_list.append({
            'symbol': symbol,
            'latest_price': float(latest_price),
            'latest_date': latest_date,
            'price_52w_high': float(price_52w_high),
            'price_52w_low': float(price_52w_low),
            'ytd_return': float(ytd_return) if ytd_return is not None else None,
            'one_year_return': float(one_year_return) if one_year_return is not None else None,
            'max_drawdown': float(max_drawdown),
            'last_price_update': latest_date,
            'days_since_update': days_since,
            'asset_class': asset_class,
            'currency': currency,
            'exchange': exchange,
            'version': 0,
            'created_at': datetime.now(timezone.utc),
            'last_updated': datetime.now(timezone.utc),
            'updated_by': 'ETL_PROCESS'
        })
    
    result_df = pd.DataFrame(metrics_list)
    logger.info(f"Calculated metrics for {len(result_df)} symbols")
    
    return result_df


def validate_metrics(df: pd.DataFrame) -> bool:
    """Validate calculated metrics."""
    required_columns = [
        'symbol', 'latest_price', 'latest_date', 'price_52w_high', 'price_52w_low',
        'asset_class', 'currency', 'version', 'created_at', 'last_updated', 'updated_by'
    ]
    
    missing_cols = set(required_columns) - set(df.columns)
    if missing_cols:
        raise ValueError(f"Missing required columns: {missing_cols}")
    
    critical_cols = ['symbol', 'latest_price', 'latest_date']
    null_in_critical = df[critical_cols].isnull().sum()
    if null_in_critical.sum() > 0:
        raise ValueError(f"Null values found in critical columns:\n{null_in_critical}")
    
    if (df['latest_price'] <= 0).any():
        raise ValueError("latest_price must be greater than 0")
    
    if (df['price_52w_high'] < df['price_52w_low']).any():
        raise ValueError("52w_high cannot be less than 52w_low")
    
    logger.info("Validation passed")
    return True


def load_to_database(db: DatabaseConnection, df: pd.DataFrame) -> int:
    """Load metrics into instruments_metrics table."""
    try:
        # Read and execute table creation SQL
        sql_file_path = os.path.join(os.path.dirname(__file__), '..', 'schema', '11-instruments_metrics.sql')
        with open(sql_file_path, 'r') as f:
            sql_content = f.read()
        
        with db.get_connection() as conn:
            with conn.cursor() as cursor:
                cursor.execute(sql_content)
                conn.commit()
            
            logger.info("instruments_metrics table ready")
            
            records_loaded = 0
            with conn.cursor() as cursor:
                for _, row in df.iterrows():
                    cursor.execute(INSERT_INSTRUMENTS_METRICS, (
                        row['symbol'],
                        float(row['latest_price']),
                        row['latest_date'],
                        float(row['price_52w_high']) if pd.notna(row['price_52w_high']) else None,
                        float(row['price_52w_low']) if pd.notna(row['price_52w_low']) else None,
                        float(row['ytd_return']) if pd.notna(row['ytd_return']) else None,
                        float(row['one_year_return']) if pd.notna(row['one_year_return']) else None,
                        float(row['max_drawdown']) if pd.notna(row['max_drawdown']) else None,
                        row['last_price_update'],
                        int(row['days_since_update']) if pd.notna(row['days_since_update']) else None,
                        row['asset_class'],
                        row['currency'],
                        row['exchange'],
                        int(row['version']),
                        row['created_at'],
                        row['last_updated'],
                        row['updated_by']
                    ))
                    records_loaded += 1
                
                conn.commit()
            
            logger.info(f"Successfully loaded {records_loaded} records to instruments_metrics")
            return records_loaded
    
    except PsycopgError as e:
        logger.error(f"Failed to load data to database: {e}")
        raise


def run_etl_pipeline() -> Dict[str, Any]:
    """Run the complete ETL pipeline."""
    start_time = datetime.now(timezone.utc)
    
    try:
        db = DatabaseConnection()
        logger.info("=" * 80)
        logger.info("Starting ETL Pipeline for Instruments Metrics")
        logger.info("=" * 80)
        
        price_df = extract_price_data(db)
        
        if price_df.empty:
            logger.warning("No price data found")
            return {
                'status': 'warning',
                'message': 'No price data to process',
                'records_loaded': 0,
                'duration_seconds': (datetime.now(timezone.utc) - start_time).total_seconds()
            }
        
        metrics_df = calculate_metrics(price_df)
        
        if metrics_df.empty:
            logger.warning("No metrics calculated")
            return {
                'status': 'warning',
                'message': 'No metrics calculated',
                'records_loaded': 0,
                'duration_seconds': (datetime.now(timezone.utc) - start_time).total_seconds()
            }
        
        validate_metrics(metrics_df)
        
        records_loaded = load_to_database(db, metrics_df)
        
        end_time = datetime.now(timezone.utc)
        duration = (end_time - start_time).total_seconds()
        
        summary = {
            'status': 'success',
            'start_time': start_time.isoformat(),
            'end_time': end_time.isoformat(),
            'duration_seconds': duration,
            'records_processed': len(metrics_df),
            'records_loaded': records_loaded,
            'unique_symbols': metrics_df['symbol'].nunique()
        }
        
        logger.info("=" * 80)
        logger.info("ETL Pipeline Completed Successfully")
        logger.info(f"Records processed: {summary['records_processed']}")
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
    parser = argparse.ArgumentParser(description='ETL pipeline for instruments metrics')
    args = parser.parse_args()
    
    result = run_etl_pipeline()
    
    if result['status'] == 'success':
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
