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
    
def get_raw_bonds(cursor):
    
    cursor.execute("""
        SELECT * FROM raw_bonds;
    """)
    
    return cursor.fetchall()

def is_valid(bond):
    if bond["nav_price"] <= 0:
        return False
    if bond["total_assets"] < 0:
        return False
    if bond["net_assets"] is not None and bond["net_assets"] < 0:
        return False
    if bond["distribution_yield"] is not None and bond["distribution_yield"] < 0:
        return False
    if bond["net_expense_ratio"] is not None and bond["net_expense_ratio"] < 0:
        return False
    return True

def insert_clean_bond(cursor, bond):
    cursor.execute("""
        INSERT INTO clean_bonds (
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
            %(distribution_yield)s
        );
    """, bond)
    
def run_etl():
    conn = get_db_connection()
       
    cursor = conn.cursor(row_factory=dict_row)
        
    bonds = get_raw_bonds(cursor)
        
    inserted = 0
    rejected = 0
    
    for bond in bonds:
        if is_valid(bond):
            insert_clean_bond(cursor, bond)
            inserted += 1
        
        else:
            rejected += 1

    conn.commit()
    cursor.close()
    conn.close()

    print(f"Inserted: {inserted}, Rejected: {rejected}")

if __name__ == "__main__":
    run_etl()