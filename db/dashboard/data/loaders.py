import os
from urllib.parse import urlparse
import pandas as pd
import psycopg
from dotenv import load_dotenv
from .kafka_consumers import get_live_prices, get_live_trades, is_kafka_available

load_dotenv()

# Database configuration
# Connection settings come from the service's SPRING_DATASOURCE_* variables;
# SPRING_DATASOURCE_URL looks like jdbc:postgresql://<host>:<port>/<database>
_datasource = urlparse(os.getenv('SPRING_DATASOURCE_URL', 'jdbc:postgresql://localhost:5432/instruments_db').removeprefix('jdbc:'))
DB_HOST = _datasource.hostname
DB_PORT = str(_datasource.port or 5432)
DB_NAME = _datasource.path.lstrip('/')
DB_USER = os.getenv('SPRING_DATASOURCE_USERNAME', 'postgres')
DB_PASSWORD = os.getenv('SPRING_DATASOURCE_PASSWORD', '')

# Analytics database configuration (for fact_trades table)
_analytics_datasource = urlparse(os.getenv('ANALYTICS_DB_URL', 'jdbc:postgresql://trade-analytics-db:5432/analytics_db').removeprefix('jdbc:'))
ANALYTICS_DB_HOST = _analytics_datasource.hostname or 'trade-analytics-db'
ANALYTICS_DB_PORT = str(_analytics_datasource.port or 5432)
ANALYTICS_DB_NAME = _analytics_datasource.path.lstrip('/') or 'analytics_db'
ANALYTICS_DB_USER = os.getenv('ANALYTICS_DB_USERNAME', 'postgres')
ANALYTICS_DB_PASSWORD = os.getenv('ANALYTICS_DB_PASSWORD', '')


def get_db_connection():
    """Create a database connection to instruments database."""
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


def get_analytics_db_connection():
    """Create a database connection to analytics database."""
    try:
        conn = psycopg.connect(
            host=ANALYTICS_DB_HOST,
            port=ANALYTICS_DB_PORT,
            dbname=ANALYTICS_DB_NAME,
            user=ANALYTICS_DB_USER,
            password=ANALYTICS_DB_PASSWORD
        )
        return conn
    except psycopg.Error as e:
        print(f"Analytics database connection failed: {e}")
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


# ============================================================================
# LIVE DATA LOADERS (from Kafka)
# ============================================================================

def get_live_prices_data() -> pd.DataFrame:
    """
    Get live price data from market-data Kafka topic.
    Returns DataFrame with columns: symbol, price, timestamp, currency
    """
    if not is_kafka_available():
        return pd.DataFrame(columns=['symbol', 'price', 'timestamp', 'currency'])
    
    prices = get_live_prices()
    if not prices:
        return pd.DataFrame(columns=['symbol', 'price', 'timestamp', 'currency'])
    
    data = []
    for symbol, price_data in prices.items():
        data.append({
            'symbol': symbol,
            'price': price_data.get('price', 0),
            'timestamp': price_data.get('timestamp', ''),
            'currency': price_data.get('currency', 'USD'),
        })
    
    df = pd.DataFrame(data)
    if not df.empty:
        df['timestamp'] = pd.to_datetime(df['timestamp'], errors='coerce')
    return df


def get_live_trade_feed_data() -> pd.DataFrame:
    """
    Get live trade events from trade-events Kafka topic.
    Returns DataFrame with columns: order_id, symbol, event_type, status, quantity, price, timestamp
    """
    if not is_kafka_available():
        return pd.DataFrame(columns=['order_id', 'symbol', 'event_type', 'status', 'quantity', 'price', 'timestamp'])
    
    trades = get_live_trades()
    if not trades:
        return pd.DataFrame(columns=['order_id', 'symbol', 'event_type', 'status', 'quantity', 'price', 'timestamp'])
    
    df = pd.DataFrame(trades)
    if not df.empty:
        df['timestamp'] = pd.to_datetime(df['timestamp'], errors='coerce')
        # Sort by timestamp descending (newest first)
        df = df.sort_values('timestamp', ascending=False)
    return df


# ============================================================================
# ANALYTICS LOADERS (from Database)
# ============================================================================

def load_trade_analytics() -> dict:
    """
    Load trade analytics from fact_trades table in analytics database.
    Returns dict with multiple analytics: volume_by_instrument, fills_vs_rejects, daily_volume
    """
    conn = get_analytics_db_connection()
    if not conn:
        return {
            'volume_by_instrument': pd.DataFrame(),
            'fills_vs_rejects': pd.DataFrame(),
            'daily_volume': pd.DataFrame(),
        }
    
    try:
        # Volume by instrument (join with dimension tables to get symbol)
        volume_by_instrument = pd.read_sql(
            """
            SELECT di.symbol, COUNT(*) as trade_count, SUM(ft.quantity) as total_quantity, 
                   AVG(ft.price) as avg_price
            FROM analytics.fact_trades ft
            JOIN analytics.dim_instrument di ON ft.instrument_key = di.instrument_key
            GROUP BY di.symbol
            ORDER BY total_quantity DESC
            LIMIT 20
            """,
            conn
        )
        
        # Fills vs Rejects by status
        fills_vs_rejects = pd.read_sql(
            """
            SELECT order_status as status, COUNT(*) as count, SUM(quantity) as total_quantity
            FROM analytics.fact_trades
            GROUP BY order_status
            ORDER BY count DESC
            """,
            conn
        )
        
        # Daily volume trend (last 30 days)
        daily_volume = pd.read_sql(
            """
            SELECT dd.date_value as trade_date, 
                   COUNT(*) as trade_count,
                   SUM(ft.quantity) as total_quantity,
                   AVG(ft.price) as avg_price
            FROM analytics.fact_trades ft
            JOIN analytics.dim_date dd ON ft.date_key = dd.date_key
            WHERE dd.date_value >= CURRENT_DATE - INTERVAL '30 days'
            GROUP BY dd.date_value
            ORDER BY trade_date DESC
            """,
            conn
        )
        
        if not daily_volume.empty:
            daily_volume['trade_date'] = pd.to_datetime(daily_volume['trade_date'])
        
        return {
            'volume_by_instrument': volume_by_instrument,
            'fills_vs_rejects': fills_vs_rejects,
            'daily_volume': daily_volume,
        }
    except psycopg.Error as e:
        print(f"Error loading trade analytics: {e}")
        return {
            'volume_by_instrument': pd.DataFrame(),
            'fills_vs_rejects': pd.DataFrame(),
            'daily_volume': pd.DataFrame(),
        }
    finally:
        conn.close()

