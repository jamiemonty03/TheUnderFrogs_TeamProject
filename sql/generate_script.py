import yfinance as yf
import psycopg
from datetime import datetime, timedelta
from dotenv import load_dotenv
import os

load_dotenv()

DAYS = 60

# Database connection from .env
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = os.getenv('DB_PORT', '5432')
DB_NAME = os.getenv('POSTGRES_DB', 'underfrog')
DB_USER = os.getenv('POSTGRES_USER', 'postgres')
DB_PASSWORD = os.getenv('POSTGRES_PASSWORD', '')

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
                'created_at': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
                'updated_by': 'SYSTEM'
            })
    
    return prices

def insert_prices_to_db(prices):
    """Insert prices directly into database using parameterized queries."""
    if not prices:
        print("No prices to insert")
        return False
    
    conn = get_db_connection()
    if not conn:
        return False
    
    try:
        cursor = conn.cursor()
        
        # Delete existing prices
        cursor.execute("DELETE FROM raw_prices;")
        
        # Insert prices using parameterized query (prevents SQL injection)
        insert_query = """
            INSERT INTO raw_prices 
            (symbol, date, open, high, low, close, volume, created_at, updated_by) 
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
        """
        
        for price in prices:
            cursor.execute(insert_query, (
                price['symbol'],
                price['date'],
                price['open'],
                price['high'],
                price['low'],
                price['close'],
                price['volume'],
                price['created_at'],
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