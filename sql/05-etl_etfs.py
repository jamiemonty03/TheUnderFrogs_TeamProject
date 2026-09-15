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

def get_raw_etfs(cursor):
    
    cursor.execute("""
        SELECT *
        FROM raw_etfs
    """)
    
    return cursor.fetchall()

def is_valid(etf):
    if etf["nav_price"] <=0:
        return False
    
    if etf["total_assets"] <= 0:
        return False

    if etf["net_assets"] is not None and etf["net_assets"] <= 0:
        return False

    if etf["net_expense_ratio"] is not None and etf["net_expense_ratio"] < 0:
        return False

    if etf["distribution_yield"] is not None and etf["distribution_yield"] < 0:
        return False

    return True

def insert_clean_etf(cursor, etf):
    
    cursor.execute("""
        INSERT INTO clean_etfs (
            symbol,
            category,
            fund_family,
            legal_type,
            net_expense_ratio,
            nav_price,
            total_assets,
            net_assets,
            ytd_return,
            three_year_avg_return,
            five_year_avg_return,
            beta_3_year,
            distribution_yield
        ) VALUES (
            %(symbol)s,
            %(category)s,
            %(fund_family)s,
            %(legal_type)s,
            %(net_expense_ratio)s,
            %(nav_price)s,
            %(total_assets)s,
            %(net_assets)s,
            %(ytd_return)s,
            %(three_year_avg_return)s,
            %(five_year_avg_return)s,
            %(beta_3_year)s,
            %(distribution_yield)s
        )
    """, etf)
    
def run_etl():
    conn = get_db_connection()
    
    cursor = conn.cursor(row_factory=dict_row)
    
    raw_etfs = get_raw_etfs(cursor)
    
    inserted = 0
    rejected = 0
    
    for etf in raw_etfs:
        if is_valid(etf):
            insert_clean_etf(cursor, etf)
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