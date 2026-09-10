import psycopg
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

def get_raw_prices():
    conn = get_db_connection()
    
    cursor = conn.cursor()
    
    cursor.execute("""
        SELECT
            r.symbol,
            r.date,
            r.open,
            r.high,
            r.low,
            r.close,
            r.volume
        FROM raw_prices r 
        LEFT JOIN clean_prices c
            ON r.symbol = c.symbol
            AND r.date = c.date
        WHERE c.symbol IS NULL
            
    """)
    
    rows = cursor.fetchall()
    
    cursor.close()
    conn.close()
    
    return rows

def is_valid(row):
    
    symbol, date, open_p, high, low, close, volume = row
    
    if open_p <= 0:
        return False
    if high <= 0:
        return False
    if low <= 0:
        return False
    if close <= 0:
        return False
    if high < low:
        return False
    if high < open_p:
        return False
    if high < close:
        return False
    if low > open_p:
        return False
    if low > close:
        return False
    if volume < 0:
        return False

    return True
    
def insert_clean_price(cursor, row):
    
    cursor.execute("""
        INSERT INTO clean_prices (symbol, date, open, high, low, close, volume)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
        
        ON CONFLICT (symbol, date) DO NOTHING
    """, row)
    
def run_etl():
    
    rows = get_raw_prices()
    
    if not rows:
        print("No new raw prices to process.")
        return
    
    conn = get_db_connection()
    
    cursor = conn.cursor()
    
    loaded = 0
    rejected = 0
    
    for row in rows:
        if is_valid(row):
            insert_clean_price(cursor, row)
            loaded += 1
        else:
            rejected += 1

    conn.commit()
    cursor.close()
    conn.close()

    print(f"ETL completed. Loaded: {loaded}, Rejected: {rejected}")
    
if __name__ == "__main__":
    run_etl()