import psycopg
from dotenv import load_dotenv
import os
from psycopg.rows import dict_row

load_dotenv()

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

def is_valid(stock):
    
    if stock["market_cap"] < 0:
        return False
    
    if stock["shares_outstanding"] < 0:
        return False
    
    if stock["full_time_employees"] is not None and stock["full_time_employees"] < 0:
        return False

    if stock["trailing_pe"] is not None and stock["trailing_pe"] < 0:
        return False

    if stock["forward_pe"] is not None and stock["forward_pe"] < 0:
        return False

    if stock["dividend_rate"] is not None and stock["dividend_rate"] < 0:
        return False

    if stock["payout_ratio"] is not None and stock["payout_ratio"] < 0:
        return False

    if stock["price_to_book"] is not None and stock["price_to_book"] < 0:
        return False

    if stock["total_revenue"] is not None and stock["total_revenue"] < 0:
        return False

    return True

def get_raw_stocks(cursor):
    cursor.execute("""
        SELECT *
        FROM raw_stocks
    """)
    return cursor.fetchall()

def insert_clean_stock(cursor, stock):
    cursor.execute("""
        INSERT INTO clean_stocks (
            symbol,
            sector,
            industry,
            country,
            market_cap,
            shares_outstanding,
            full_time_employees,
            trailing_pe,
            forward_pe,
            trailing_eps,
            dividend_rate,
            payout_ratio,
            price_to_book,
            return_on_equity,
            total_revenue,
            website
        ) VALUES (
            %(symbol)s,
            %(sector)s,
            %(industry)s,
            %(country)s,
            %(market_cap)s,
            %(shares_outstanding)s,
            %(full_time_employees)s,
            %(trailing_pe)s,
            %(forward_pe)s,
            %(trailing_eps)s,
            %(dividend_rate)s,
            %(payout_ratio)s,
            %(price_to_book)s,
            %(return_on_equity)s,
            %(total_revenue)s,
            %(website)s
        )
    """, stock)
    
def run_etl():
    conn = get_db_connection()
    
    cursor = conn.cursor(row_factory=dict_row)
    
    raw_stocks = get_raw_stocks(cursor)
    
    inserted = 0
    rejected = 0
    
    for stock in raw_stocks:
        if is_valid(stock):
            insert_clean_stock(cursor, stock)
            inserted += 1
        else:
            rejected += 1

    conn.commit()
    cursor.close()
    conn.close()

    print(f"Inserted: {inserted}")
    print(f"Rejected: {rejected}")
    
    
if __name__ == "__main__":
    run_etl()