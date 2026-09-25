import yfinance as yf
import psycopg
from datetime import datetime, timedelta
from dotenv import load_dotenv
import os
from urllib.parse import urlparse

load_dotenv()

DAYS = 365

# Database connection from .env
# Connection settings come from the service's SPRING_DATASOURCE_* variables;
# SPRING_DATASOURCE_URL looks like jdbc:postgresql://<host>:<port>/<database>
_datasource = urlparse(os.getenv('SPRING_DATASOURCE_URL', 'jdbc:postgresql://localhost:5432/instruments_db').removeprefix('jdbc:'))
DB_HOST = _datasource.hostname
DB_PORT = str(_datasource.port or 5432)
DB_NAME = _datasource.path.lstrip('/')
DB_USER = os.getenv('SPRING_DATASOURCE_USERNAME', 'postgres')
DB_PASSWORD = os.getenv('SPRING_DATASOURCE_PASSWORD', '')

def get_db_connection():
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

def load_instruments():
    """Query instruments table from database."""
    conn = get_db_connection()
    if not conn:
        return []
    
    try:
        cursor = conn.cursor()
        cursor.execute("SELECT symbol FROM instruments ORDER BY symbol;")
        symbols = [row[0] for row in cursor.fetchall()]
        cursor.close()
        
        print(f"Loaded {len(symbols)} instruments from database")
        return symbols
    except psycopg.Error as e:
        print(f"Error loading instruments: {e}")
        return []
    finally:
        conn.close()

def fetch_yfinance_data(symbols, days=DAYS):
    """Download OHLCV data from Yahoo Finance."""
    end_date = datetime.now()
    start_date = end_date - timedelta(days=days)
    
    data = {}
    for symbol in symbols:
        try:
            df = yf.download(symbol, start=start_date, end=end_date, progress=False)
            if len(df) > 0:
                data[symbol] = df
                print(f"✓ Downloaded {symbol}: {len(df)} days")
            else:
                print(f"✗ No data for {symbol}")
        except Exception as e:
            print(f"✗ Error fetching {symbol}: {e}")
    
    return data


def generate_prices(yf_data):
    """Convert yfinance data to prices table rows."""
    prices = []
    now = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    
    for symbol, df in yf_data.items():
        for date_index, row in df.iterrows():
            prices.append({
                'symbol': symbol,
                'date': date_index.strftime('%Y-%m-%d'),
                'open': round(float(row['Open'].item()), 4),
                'high': round(float(row['High'].item()), 4),
                'low': round(float(row['Low'].item()), 4),
                'close': round(float(row['Close'].item()), 4),
                'volume': int(row['Volume'].item()),
                'version': 0,
                'created_at': now,
                'last_updated': now,
                'updated_by': 'SYSTEM'
            })
    
    return prices

def insert_prices_to_db(prices):
    """Insert prices using upsert (ON CONFLICT) to track version increments."""
    if not prices:
        print("No prices to insert")
        return False
    
    conn = get_db_connection()
    if not conn:
        return False
    
    try:
        cursor = conn.cursor()
        
        # Upsert query: insert new records, update existing ones with version increment
        upsert_query = """
            INSERT INTO raw_prices 
            (symbol, date, open, high, low, close, volume, version, created_at, last_updated, updated_by) 
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (symbol, date) 
            DO UPDATE SET 
                open = EXCLUDED.open,
                high = EXCLUDED.high,
                low = EXCLUDED.low,
                close = EXCLUDED.close,
                volume = EXCLUDED.volume,
                version = raw_prices.version + 1,
                last_updated = EXCLUDED.last_updated,
                updated_by = EXCLUDED.updated_by
        """
        
        for price in prices:
            cursor.execute(upsert_query, (
                price['symbol'],
                price['date'],
                price['open'],
                price['high'],
                price['low'],
                price['close'],
                price['volume'],
                price['version'],
                price['created_at'],
                price['last_updated'],
                price['updated_by']
            ))
        
        conn.commit()
        return True
        
    except psycopg.Error as e:
        conn.rollback()
        print(f"Error inserting prices: {e}")
        return False
    finally:
        cursor.close()
        conn.close()

def main():
    symbols = load_instruments()
    
    if not symbols:
        print("No instruments found. Exiting.")
        return
    
    yf_data = fetch_yfinance_data(symbols)
    
    if not yf_data:
        print("No yfinance data available. Exiting.")
        return
    

    prices = generate_prices(yf_data)
    
    if insert_prices_to_db(prices):
        print("\nPrice generation and database insertion complete!")
    else:
        print("\nFailed to insert prices into database")

if __name__ == '__main__':
    main()