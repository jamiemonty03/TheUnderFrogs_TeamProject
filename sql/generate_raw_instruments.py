import yfinance as yf
import pandas as pd
import psycopg
from dotenv import load_dotenv
import os

load_dotenv()

# Database connection from .env
DB_HOST = os.getenv('DB_HOST', 'localhost')
DB_PORT = os.getenv('DB_PORT', '5432')
DB_NAME = os.getenv('POSTGRES_DB', 'underfrog')
DB_USER = os.getenv('POSTGRES_USER', 'postgres')
DB_PASSWORD = os.getenv('POSTGRES_PASSWORD', '')


STOCKS = ["AAPL", "MSFT", "JPM", "TSLA", "GOOGL", "AMZN", "NVDA", "XOM", "JNJ", "KO"]
ETFS = ["SPY", "QQQ", "VTI", "IWM", "DIA", "EFA", "EEM", "XLF", "XLK", "XLE"]
BONDS = ["AGG", "TLT", "LQD", "BND", "SHY", "IEF", "HYG", "MUB", "TIP", "BNDX"]
TICKERS = STOCKS + ETFS + BONDS

# yfinance's quoteType tells stocks (EQUITY) apart from everything fund-shaped
# (ETF), but a bond fund and an equity fund both just say "ETF" - the
# category text (e.g. "Intermediate Core Bond", "Long Government") is what
# actually distinguishes them.
BOND_CATEGORY_KEYWORDS = [
    "bond", "government", "treasury", "muni", "inflation-protected",
    "debt", "bank loan", "target maturity",
]


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
    conn = get_db_connection()
    if not conn:
        return []

    try:
        cursor = conn.cursor()
        cursor.execute("SELECT symbol FROM instruments;")
        symbols = [row[0] for row in cursor.fetchall()]
        cursor.close()
        return symbols
    except psycopg.Error as e:
        print(f"Error loading existing instruments: {e}")
        return []
    finally:
        conn.close()


def fetch_yfinance_data(tickers):
    """Download yfinance .info for each ticker."""
    data = {}
    for symbol in tickers:
        try:
            info = yf.Ticker(symbol).info
            if not info or info.get("quoteType") is None:
                print(f"No data for {symbol}")
                continue
            data[symbol] = info
            print(f"Fetched {symbol}: quoteType={info.get('quoteType')}")
        except Exception as e:
            print(f"Error fetching {symbol}: {e}")

    return data


def classify_instrument(info: dict) -> str:
    quote_type = info.get("quoteType")
    category = (info.get("category") or "").lower()

    if quote_type == "EQUITY":
        return "Equity"
    if quote_type == "ETF":
        return "Bond" if any(kw in category for kw in BOND_CATEGORY_KEYWORDS) else "ETF"
    return "unknown"


def build_stock_row(symbol, info):
    return {
        "symbol": symbol,
        "sector": info.get("sector"),
        "industry": info.get("industry"),
        "country": info.get("country"),
        "market_cap": info.get("marketCap"),
        "shares_outstanding": info.get("sharesOutstanding"),
        "full_time_employees": info.get("fullTimeEmployees"),
        "beta": info.get("beta"),
        "trailing_pe": info.get("trailingPE"),
        "forward_pe": info.get("forwardPE"),
        "trailing_eps": info.get("trailingEps"),
        "dividend_rate": info.get("dividendRate"),
        "payout_ratio": info.get("payoutRatio"),
        "price_to_book": info.get("priceToBook"),
        "return_on_equity": info.get("returnOnEquity"),
        "total_revenue": info.get("totalRevenue"),
        "website": info.get("website"),
    }


def build_fund_row(symbol, info):
    """Shared by etfs and bonds - both are fund-shaped in yfinance and have
    identical schemas. """
    return {
        "symbol": symbol,
        "category": info.get("category"),
        "fund_family": info.get("fundFamily"),
        "legal_type": info.get("legalType"),
        "net_expense_ratio": info.get("netExpenseRatio"),
        "nav_price": info.get("navPrice"),
        "total_assets": info.get("totalAssets"),
        "net_assets": info.get("netAssets"),
        "ytd_return": info.get("ytdReturn"),
        "three_year_avg_return": info.get("threeYearAverageReturn"),
        "five_year_avg_return": info.get("fiveYearAverageReturn"),
        "beta_3_year": info.get("beta3Year"),
        "distribution_yield": info.get("yield"),
    }


def clean_dataframe(df: pd.DataFrame) -> pd.DataFrame:
    """Swap pandas' NaN (from missing yfinance fields) back to real None,
    so psycopg inserts NULL instead of the string 'NaN'."""
    return df.astype(object).where(pd.notnull(df), None)


def build_dataframes(yf_data: dict):
    """Classify each symbol and split it into instruments_df plus one
    DataFrame per asset class."""
    instrument_rows, stock_rows, etf_rows, bond_rows = [], [], [], []

    for symbol, info in yf_data.items():
        asset_class = classify_instrument(info)
        if asset_class == "unknown":
            print(f"✗ {symbol}: quoteType={info.get('quoteType')!r} doesn't map to stock/etf/bond, skipping")
            continue

        instrument_rows.append({
            "symbol": symbol,
            "name": info.get("shortName") or info.get("longName") or symbol,
            "asset_class": asset_class,
            "currency": info.get("currency"),
            "exchange": info.get("fullExchangeName"),
        })

        if asset_class == "Equity":
            stock_rows.append(build_stock_row(symbol, info))
        elif asset_class == "ETF":
            etf_rows.append(build_fund_row(symbol, info))
        elif asset_class == "Bond":
            bond_rows.append(build_fund_row(symbol, info))

    instruments_df, stocks_df, etfs_df, bonds_df = (
        clean_dataframe(pd.DataFrame(rows))
        for rows in (instrument_rows, stock_rows, etf_rows, bond_rows)
    )

    print(f"Built instruments_df ({len(instruments_df)} rows), "
          f"stocks_df ({len(stocks_df)}), etfs_df ({len(etfs_df)}), bonds_df ({len(bonds_df)})")

    return instruments_df, stocks_df, etfs_df, bonds_df


def insert_to_db(instruments_df, stocks_df, etfs_df, bonds_df) -> bool:
    conn = get_db_connection()
    if not conn:
        return False

    try:
        cursor = conn.cursor()

        for row in instruments_df.itertuples(index=False):
            cursor.execute("""
                INSERT INTO instruments (symbol, name, asset_class, currency, exchange, tradable)
                VALUES (%s, %s, %s, %s, %s, TRUE)
                ON CONFLICT (symbol) DO UPDATE SET
                    name = EXCLUDED.name,
                    asset_class = EXCLUDED.asset_class,
                    currency = EXCLUDED.currency,
                    exchange = EXCLUDED.exchange,
                    last_updated = NOW()
            """, (row.symbol, row.name, row.asset_class, row.currency, row.exchange))
        print(f"Upserted {len(instruments_df)} rows into instruments")

        for table, df in [("raw_stocks", stocks_df), ("raw_etfs", etfs_df), ("raw_bonds", bonds_df)]:
            cursor.execute(f"DELETE FROM {table};")
            if not df.empty:
                col_list = ", ".join(df.columns)
                placeholders = ", ".join(["%s"] * len(df.columns))
                insert_query = f"INSERT INTO {table} ({col_list}) VALUES ({placeholders})"
                for row in df.itertuples(index=False):
                    cursor.execute(insert_query, tuple(row))
            print(f"Inserted {len(df)} rows into {table}")

        conn.commit()
        return True

    except psycopg.Error as e:
        conn.rollback()
        print(f"Error inserting instrument data: {e}")
        return False
    finally:
        cursor.close()
        conn.close()


def main():
    yf_data = fetch_yfinance_data(TICKERS)

    if not yf_data:
        print("No yfinance data available. Exiting.")
        return

    extra_symbols = [s for s in load_instruments() if s not in yf_data]
    if extra_symbols:
        print(f"Also fetching {len(extra_symbols)} instruments already in the DB: {extra_symbols}")
        yf_data.update(fetch_yfinance_data(extra_symbols))

    instruments_df, stocks_df, etfs_df, bonds_df = build_dataframes(yf_data)

    if insert_to_db(instruments_df, stocks_df, etfs_df, bonds_df):
        print("\nInstrument table generation and database insertion complete!")
    else:
        print("\nFailed to insert instrument data into database")


if __name__ == '__main__':
    main()