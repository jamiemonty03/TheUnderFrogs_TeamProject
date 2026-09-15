import os
import pandas as pd
import psycopg
from dotenv import load_dotenv

load_dotenv()

# Database configuration
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = os.getenv('DB_PORT', '5432')
DB_NAME = os.getenv('POSTGRES_DB', 'underfrog')
DB_USER = os.getenv('POSTGRES_USER', 'postgres')
DB_PASSWORD = os.getenv('POSTGRES_PASSWORD', '')


def get_db_connection():
    """Create a database connection."""
    try:
        conn = psycopg.connect(
            host=DB_HOST,
            port=DB_PORT,
            dbname=DB_NAME,
            user=DB_USER,
            password=DB_PASSWORD
        )
        return conn
    except psycopg.Error as e:
        print(f"Database connection failed: {e}")
        return None


def load_price_metrics() -> pd.DataFrame:
    """Load all price metrics from database."""
    conn = get_db_connection()
    if not conn:
        return pd.DataFrame()
    
    try:
        df = pd.read_sql(
            "SELECT * FROM price_metrics ORDER BY ticker, trade_date",
            conn
        )
        df['trade_date'] = pd.to_datetime(df['trade_date'])
        return df
    finally:
        conn.close()


def load_instruments_metrics() -> pd.DataFrame:
    """Load instruments metrics for screener."""
    conn = get_db_connection()
    if not conn:
        return pd.DataFrame()
    
    try:
        df = pd.read_sql(
            "SELECT symbol, latest_price, latest_date, price_52w_high, price_52w_low, "
            "ytd_return, one_year_return, max_drawdown, days_since_update, "
            "asset_class, currency, exchange FROM instruments_metrics ORDER BY symbol",
            conn
        )
        df['latest_date'] = pd.to_datetime(df['latest_date'])
        return df
    finally:
        conn.close()


def get_available_tickers(df: pd.DataFrame) -> list:
    """Get list of available tickers from price metrics."""
    return sorted(df['ticker'].unique().tolist()) if not df.empty else []


def get_available_asset_classes(df: pd.DataFrame) -> list:
    """Get list of available asset classes."""
    return sorted(df['asset_class'].unique().tolist()) if not df.empty else []


def get_available_currencies(df: pd.DataFrame) -> list:
    """Get list of available currencies."""
    return sorted(df['currency'].unique().tolist()) if not df.empty else []
