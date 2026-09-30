import os
import sys
import json
import logging
from datetime import datetime, timedelta
from typing import Optional, Dict, List, Tuple
from urllib.parse import urlparse
import psycopg
from psycopg import sql
from dotenv import load_dotenv

load_dotenv()

logging.basicConfig(
    level=os.getenv('LOG_LEVEL', 'INFO'),
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class DatabaseConnection:
    def __init__(self, db_name: str, max_retries: int = 3):
        self.db_name = db_name
        self.max_retries = max_retries
        self.connection = None
        self._parse_credentials()

    def _parse_credentials(self):
        env_prefix = f"{self.db_name.upper()}_DB"
        url_str = os.getenv(f"{env_prefix}_URL", "")
        
        if not url_str:
            raise ValueError(f"Missing {env_prefix}_URL in environment")
        
        if url_str.startswith("jdbc:"):
            url_str = url_str[5:]
        
        parsed = urlparse(url_str)
        self.host = parsed.hostname or "localhost"
        self.port = parsed.port or 5432
        self.database = parsed.path.lstrip("/") or self.db_name.lower()
        self.user = os.getenv(f"{env_prefix}_USERNAME")
        self.password = os.getenv(f"{env_prefix}_PASSWORD", "")
        
        if not self.user:
            raise ValueError(f"Missing {env_prefix}_USERNAME in environment")

    def connect(self):
        for attempt in range(self.max_retries):
            try:
                self.connection = psycopg.connect(
                    host=self.host,
                    port=self.port,
                    dbname=self.database,
                    user=self.user,
                    password=self.password,
                    connect_timeout=10
                )
                logger.info(f"Connected to {self.db_name} database")
                return self.connection
            except psycopg.OperationalError as e:
                wait_time = 2 ** attempt
                if attempt < self.max_retries - 1:
                    logger.warning(
                        f"Connection attempt {attempt + 1}/{self.max_retries} failed for {self.db_name}. "
                        f"Retrying in {wait_time} seconds: {e}"
                    )
                    import time
                    time.sleep(wait_time)
                else:
                    logger.error(f"All {self.max_retries} connection attempts failed for {self.db_name}")
                    raise

    def get_cursor(self):
        if not self.connection:
            self.connect()
        return self.connection.cursor()

    def commit(self):
        if self.connection:
            self.connection.commit()

    def rollback(self):
        if self.connection:
            self.connection.rollback()

    def close(self):
        if self.connection:
            self.connection.close()
            logger.info(f"Closed connection to {self.db_name}")


class TradeDataETL:
    def __init__(self):
        self.batch_id = datetime.now().isoformat()
        self.orders_db = DatabaseConnection("orders")
        self.accounts_db = DatabaseConnection("accounts")
        self.instruments_db = DatabaseConnection("instruments")
        self.analytics_db = DatabaseConnection("analytics")
        self.row_count_loaded = 0
        self.row_count_dead_lettered = 0

    def read_watermark(self) -> Tuple[Optional[datetime], Optional[str]]:
        try:
            cursor = self.analytics_db.get_cursor()
            cursor.execute(
                "SELECT high_watermark, watermark_id FROM analytics.etl_watermark "
                "ORDER BY created_at DESC LIMIT 1"
            )
            result = cursor.fetchone()
            if result:
                return result[0], result[1]
            return None, None
        except Exception as e:
            logger.warning(f"Failed to read watermark: {e}. Using default (90 days ago)")
            return None, None

    def initialize_watermark(self) -> datetime:
        default_watermark = datetime.now() - timedelta(days=90)
        logger.info(f"Initializing watermark to {default_watermark}")
        return default_watermark

    def extract_orders(self, high_watermark: datetime) -> List[Dict]:
        orders = []
        try:
            cursor = self.orders_db.get_cursor()
            
            new_orders_query = sql.SQL(
                "SELECT order_id, idempotency_key, account_id, symbol, side, quantity, price, "
                "order_status, created_at, version "
                "FROM orders "
                "WHERE created_at > %s "
                "ORDER BY created_at ASC"
            )
            cursor.execute(new_orders_query, (high_watermark,))
            new_orders = cursor.fetchall()
            
            if new_orders:
                logger.info(f"Extracted {len(new_orders)} new orders since {high_watermark}")
                orders.extend(new_orders)
            
            analytics_cursor = self.analytics_db.get_cursor()
            analytics_cursor.execute(
                "SELECT trade_key FROM analytics.fact_trades WHERE order_status = 'NEW'"
            )
            still_new_keys = [row[0] for row in analytics_cursor.fetchall()]
            analytics_cursor.close()

            
            reprocess_query = sql.SQL(
                "SELECT order_id, idempotency_key, account_id, symbol, side, quantity, price, "
                "order_status, created_at, version "
                "FROM orders "
                "WHERE order_id = ANY(%s) AND created_at <= %s"
            )
            cursor.execute(reprocess_query, (still_new_keys, high_watermark))
            reprocess_orders = cursor.fetchall()
            
            if reprocess_orders:
                logger.info(f"Re-processing {len(reprocess_orders)} orders that were still NEW")
                orders.extend(reprocess_orders)
            
            cursor.close()
            return orders
        except Exception as e:
            logger.error(f"Failed to extract orders: {e}")
            raise

    def validate_order(self, order: Dict) -> Tuple[bool, Optional[str]]:
        order_id, idempotency_key, account_id, symbol, side, quantity, price, order_status, created_at, version = order
        
        if quantity <= 0:
            return False, "quantity must be > 0"
        if price <= 0:
            return False, "price must be > 0"
        if side not in ('BUY', 'SELL'):
            return False, f"invalid side: {side}"
        if order_status not in ('NEW', 'FILLED', 'REJECTED', 'CANCELLED'):
            return False, f"invalid order_status: {order_status}"
        
        try:
            cursor = self.accounts_db.get_cursor()
            cursor.execute("SELECT 1 FROM accounts WHERE account_id = %s", (account_id,))
            if not cursor.fetchone():
                cursor.close()
                return False, f"account not found: {account_id}"
            cursor.close()
        except Exception as e:
            logger.error(f"Failed to validate account {account_id}: {e}")
            raise
        
        try:
            cursor = self.instruments_db.get_cursor()
            cursor.execute("SELECT 1 FROM instruments WHERE symbol = %s", (symbol,))
            if not cursor.fetchone():
                cursor.close()
                return False, f"instrument not found: {symbol}"
            cursor.close()
        except Exception as e:
            logger.error(f"Failed to validate instrument {symbol}: {e}")
            raise
        
        return True, None

    def record_dead_letter(self, source_row: Dict, reason: str):
        try:
            cursor = self.analytics_db.get_cursor()
            cursor.execute(
                "INSERT INTO analytics.etl_dead_letter "
                "(batch_id, source_table, source_row, reason) "
                "VALUES (%s, %s, %s, %s)",
                # default=str keeps Decimal prices exact (as strings) instead of failing to serialise
                (self.batch_id, 'orders', json.dumps(source_row, default=str), reason)
            )
            self.row_count_dead_lettered += 1
            cursor.close()
        except Exception as e:
            logger.error(f"Failed to record dead letter for {source_row}: {e}")
            raise

    def upsert_dimensions(self, orders: List[Dict]):
        cursor = self.analytics_db.get_cursor()
        
        try:
            accounts = {}
            instruments = {}
            dates = set()
            
            for order in orders:
                order_id, idempotency_key, account_id, symbol, side, quantity, price, order_status, created_at, version = order
                
                if account_id not in accounts:
                    acct_cursor = self.accounts_db.get_cursor()
                    acct_cursor.execute(
                        "SELECT account_id, holder_name, cash_balance, status, user_id "
                        "FROM accounts WHERE account_id = %s",
                        (account_id,)
                    )
                    row = acct_cursor.fetchone()
                    if row:
                        accounts[account_id] = row
                    acct_cursor.close()
                
                if symbol not in instruments:
                    inst_cursor = self.instruments_db.get_cursor()
                    inst_cursor.execute(
                        "SELECT symbol, name, asset_class, currency, exchange, tradable, version "
                        "FROM instruments WHERE symbol = %s",
                        (symbol,)
                    )
                    row = inst_cursor.fetchone()
                    if row:
                        instruments[symbol] = row
                    inst_cursor.close()
                
                dates.add(created_at.date())
            
            for account_id, acct_row in accounts.items():
                cursor.execute(
                    "INSERT INTO analytics.DIM_ACCOUNT "
                    "(account_id, holder_name, cash_balance, account_status, user_id, last_updated) "
                    "VALUES (%s, %s, %s, %s, %s, NOW()) "
                    "ON CONFLICT (account_id) DO UPDATE SET "
                    "holder_name = EXCLUDED.holder_name, "
                    "cash_balance = EXCLUDED.cash_balance, "
                    "account_status = EXCLUDED.account_status, "
                    "last_updated = NOW()",
                    acct_row
                )
            
            for symbol, inst_row in instruments.items():
                cursor.execute(
                    "INSERT INTO analytics.DIM_INSTRUMENT "
                    "(symbol, name, asset_class, currency, exchange, tradable, version, last_updated) "
                    "VALUES (%s, %s, %s, %s, %s, %s, %s, NOW()) "
                    "ON CONFLICT (symbol) DO UPDATE SET "
                    "name = EXCLUDED.name, "
                    "asset_class = EXCLUDED.asset_class, "
                    "currency = EXCLUDED.currency, "
                    "exchange = EXCLUDED.exchange, "
                    "tradable = EXCLUDED.tradable, "
                    "version = EXCLUDED.version, "
                    "last_updated = NOW()",
                    inst_row
                )
            
            for date_val in dates:
                year = date_val.year
                quarter = (date_val.month - 1) // 3 + 1
                month = date_val.month
                day = date_val.day
                day_of_week = date_val.weekday() + 1
                week_of_year = date_val.isocalendar()[1]
                day_name = date_val.strftime('%A')
                month_name = date_val.strftime('%B')
                is_weekend = date_val.weekday() >= 5
                date_key = int(date_val.strftime('%Y%m%d'))
                
                cursor.execute(
                    "INSERT INTO analytics.DIM_DATE "
                    "(date_key, date_value, year, quarter, month, day, day_of_week, week_of_year, "
                    "day_name, month_name, is_weekend) "
                    "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) "
                    "ON CONFLICT (date_key) DO NOTHING",
                    (date_key, date_val, year, quarter, month, day, day_of_week, week_of_year,
                     day_name, month_name, is_weekend)
                )
            
            logger.info(f"Upserted {len(accounts)} accounts, {len(instruments)} instruments, {len(dates)} dates")
            cursor.close()
        except Exception as e:
            cursor.close()
            logger.error(f"Failed to upsert dimensions: {e}")
            raise

    def merge_facts(self, orders: List[Dict]):
        cursor = self.analytics_db.get_cursor()
        
        try:
            for order in orders:
                order_id, idempotency_key, account_id, symbol, side, quantity, price, order_status, created_at, version = order
                
                account_cursor = self.analytics_db.get_cursor()
                account_cursor.execute(
                    "SELECT account_key FROM analytics.DIM_ACCOUNT WHERE account_id = %s",
                    (account_id,)
                )
                account_key = account_cursor.fetchone()[0]
                account_cursor.close()
                
                instrument_cursor = self.analytics_db.get_cursor()
                instrument_cursor.execute(
                    "SELECT instrument_key FROM analytics.DIM_INSTRUMENT WHERE symbol = %s",
                    (symbol,)
                )
                instrument_key = instrument_cursor.fetchone()[0]
                instrument_cursor.close()
                
                date_key = int(created_at.strftime('%Y%m%d'))
                
                cursor.execute(
                    "INSERT INTO analytics.FACT_TRADES "
                    "(trade_key, account_key, instrument_key, date_key, side, quantity, price, order_status, last_updated) "
                    "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, NOW()) "
                    "ON CONFLICT (trade_key) DO UPDATE SET "
                    "side = EXCLUDED.side, "
                    "quantity = EXCLUDED.quantity, "
                    "price = EXCLUDED.price, "
                    "order_status = EXCLUDED.order_status, "
                    "last_updated = NOW()",
                    (order_id, account_key, instrument_key, date_key, side, quantity, price, order_status)
                )
                self.row_count_loaded += 1
            
            logger.info(f"Merged {self.row_count_loaded} trades into FACT_TRADES")
            cursor.close()
        except Exception as e:
            cursor.close()
            logger.error(f"Failed to merge facts: {e}")
            raise

    def update_watermark(self, high_watermark: datetime):
        try:
            cursor = self.analytics_db.get_cursor()
            cursor.execute(
                "INSERT INTO analytics.etl_watermark "
                "(high_watermark, batch_id, batch_start_time, batch_end_time, row_count_loaded, row_count_dead_lettered) "
                "VALUES (%s, %s, %s, %s, %s, %s)",
                (high_watermark, self.batch_id, datetime.now(), datetime.now(), 
                 self.row_count_loaded, self.row_count_dead_lettered)
            )
            cursor.close()
            logger.info(f"Updated watermark to {high_watermark}")
        except Exception as e:
            logger.error(f"Failed to update watermark: {e}")
            raise

    def run(self):
        try:
            self.orders_db.connect()
            self.accounts_db.connect()
            self.instruments_db.connect()
            self.analytics_db.connect()
            
            logger.info(f"Starting trade data ETL with batch_id={self.batch_id}")
            
            high_watermark, watermark_id = self.read_watermark()
            if not high_watermark:
                high_watermark = self.initialize_watermark()
            
            logger.info(f"Using watermark: {high_watermark}")
            
            orders = self.extract_orders(high_watermark)
            if not orders:
                logger.info("No orders to process")
                return
            
            valid_orders = []
            for order in orders:
                is_valid, reason = self.validate_order(order)
                if is_valid:
                    valid_orders.append(order)
                else:
                    order_id, idempotency_key, account_id, symbol, side, quantity, price, order_status, created_at, version = order
                    order_dict = {
                        'order_id': order_id,
                        'account_id': account_id,
                        'symbol': symbol,
                        'side': side,
                        'quantity': quantity,
                        'price': price,
                        'order_status': order_status
                    }
                    self.record_dead_letter(order_dict, reason)
                    logger.warning(f"Dead-lettering order {order[0]}: {reason}")
            
            logger.info(f"Extracted {len(orders)} orders, {len(valid_orders)} valid, {self.row_count_dead_lettered} dead-lettered")

            # Still fall through to the commit when every order failed, so the dead letters are saved
            if valid_orders:
                self.upsert_dimensions(valid_orders)
                self.merge_facts(valid_orders)
            else:
                logger.info("All extracted orders failed validation")

            # Count dead-lettered orders too so they aren't re-extracted, and never move backwards
            # (re-processed NEW orders are older than the current watermark)
            new_watermark = max([high_watermark] + [order[8] for order in orders])
            self.update_watermark(new_watermark)
            
            self.analytics_db.commit()
            logger.info(f"ETL completed successfully. Loaded {self.row_count_loaded} trades, "
                       f"dead-lettered {self.row_count_dead_lettered}")
            
        except Exception as e:
            logger.error(f"ETL failed: {e}")
            self.analytics_db.rollback()
            raise
        finally:
            self.orders_db.close()
            self.accounts_db.close()
            self.instruments_db.close()
            self.analytics_db.close()


def main():
    try:
        etl = TradeDataETL()
        etl.run()
    except Exception as e:
        logger.error(f"Fatal error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
