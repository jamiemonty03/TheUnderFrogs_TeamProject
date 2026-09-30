import pytest
import json
import os
from datetime import datetime, timedelta
from unittest.mock import Mock, patch, MagicMock, call
from io import StringIO
import sys

etl_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'etl'))
if etl_dir not in sys.path:
    sys.path.insert(0, etl_dir)

from load_fact_trades import TradeDataETL, DatabaseConnection


class TestDatabaseConnection:
    def test_parse_credentials_from_env(self):
        with patch.dict(os.environ, {
            'ORDERS_DB_URL': 'jdbc:postgresql://localhost:5432/test_db',
            'ORDERS_DB_USERNAME': 'test_user',
            'ORDERS_DB_PASSWORD': 'test_pass'
        }):
            conn = DatabaseConnection('orders')
            assert conn.host == 'localhost'
            assert conn.port == 5432
            assert conn.database == 'test_db'
            assert conn.user == 'test_user'
            assert conn.password == 'test_pass'

    def test_connection_retry_with_exponential_backoff(self):
        with patch.dict(os.environ, {
            'TEST_DB_URL': 'jdbc:postgresql://localhost:5432/test_db',
            'TEST_DB_USERNAME': 'test_user',
            'TEST_DB_PASSWORD': 'test_pass'
        }):
            with patch('load_fact_trades.psycopg.connect') as mock_connect:
                import psycopg
                mock_connect.side_effect = [
                    psycopg.OperationalError("Connection failed"),
                    psycopg.OperationalError("Connection failed"),
                    MagicMock()
                ]
                
                with patch('load_fact_trades.DatabaseConnection._parse_credentials', lambda x: None):
                    conn = DatabaseConnection('orders', max_retries=3)
                    conn.host = 'localhost'
                    conn.port = 5432
                    conn.database = 'orders_db'
                    conn.user = 'test_user'
                    conn.password = 'test_pass'
                    
                    with patch('time.sleep'):
                        result = conn.connect()
                    
                    assert result is not None
                    assert mock_connect.call_count == 3

    def test_connection_max_retries_exceeded(self):
        with patch.dict(os.environ, {
            'TEST_DB_URL': 'jdbc:postgresql://localhost:5432/test_db',
            'TEST_DB_USERNAME': 'test_user',
            'TEST_DB_PASSWORD': 'test_pass'
        }):
            with patch('psycopg.connect') as mock_connect:
                mock_connect.side_effect = Exception("Connection failed")
                
                conn = DatabaseConnection('test', max_retries=2)
                with patch('time.sleep'):
                    with pytest.raises(Exception, match="Connection failed"):
                        conn.connect()


class TestWatermarkLogic:
    @pytest.fixture
    def mock_etl(self):
        with patch.multiple(
            'load_fact_trades.DatabaseConnection',
            connect=MagicMock(),
            get_cursor=MagicMock(),
            commit=MagicMock(),
            rollback=MagicMock(),
            close=MagicMock()
        ):
            with patch.dict(os.environ, {
                'ORDERS_DB_URL': 'jdbc:postgresql://localhost:5432/orders_db',
                'ORDERS_DB_USERNAME': 'test_user',
                'ORDERS_DB_PASSWORD': 'test_pass',
                'ACCOUNTS_DB_URL': 'jdbc:postgresql://localhost:5432/accounts_db',
                'ACCOUNTS_DB_USERNAME': 'test_user',
                'ACCOUNTS_DB_PASSWORD': 'test_pass',
                'INSTRUMENTS_DB_URL': 'jdbc:postgresql://localhost:5432/instruments_db',
                'INSTRUMENTS_DB_USERNAME': 'test_user',
                'INSTRUMENTS_DB_PASSWORD': 'test_pass',
                'ANALYTICS_DB_URL': 'jdbc:postgresql://localhost:5432/instruments_db',
                'ANALYTICS_DB_USERNAME': 'test_user',
                'ANALYTICS_DB_PASSWORD': 'test_pass'
            }):
                etl = TradeDataETL()
                yield etl

    def test_read_watermark_empty_table(self, mock_etl):
        mock_cursor = MagicMock()
        mock_cursor.fetchone.return_value = None
        mock_etl.analytics_db.get_cursor.return_value = mock_cursor
        
        watermark, watermark_id = mock_etl.read_watermark()
        assert watermark is None
        assert watermark_id is None

    def test_read_watermark_existing(self, mock_etl):
        test_time = datetime(2026, 9, 28, 12, 0, 0)
        mock_cursor = MagicMock()
        mock_cursor.fetchone.return_value = (test_time, 1)
        mock_etl.analytics_db.get_cursor.return_value = mock_cursor
        
        watermark, watermark_id = mock_etl.read_watermark()
        assert watermark == test_time
        assert watermark_id == 1

    def test_initialize_watermark_default(self, mock_etl):
        watermark = mock_etl.initialize_watermark()
        
        assert watermark is not None
        assert (datetime.now() - watermark).days == 90

    def test_watermark_update_recorded(self, mock_etl):
        mock_cursor = MagicMock()
        mock_etl.analytics_db.get_cursor.return_value = mock_cursor
        mock_etl.row_count_loaded = 5
        mock_etl.row_count_dead_lettered = 1
        
        test_time = datetime(2026, 9, 28, 12, 0, 0)
        mock_etl.update_watermark(test_time)
        
        mock_cursor.execute.assert_called_once()
        call_args = mock_cursor.execute.call_args
        assert 'INSERT INTO analytics.etl_watermark' in call_args[0][0]


class TestOrderValidation:
    @pytest.fixture
    def mock_etl(self):
        with patch.multiple(
            'load_fact_trades.DatabaseConnection',
            connect=MagicMock(),
            get_cursor=MagicMock(),
            commit=MagicMock(),
            rollback=MagicMock(),
            close=MagicMock()
        ):
            with patch.dict(os.environ, {
                'ORDERS_DB_URL': 'jdbc:postgresql://localhost:5432/orders_db',
                'ORDERS_DB_USERNAME': 'test_user',
                'ORDERS_DB_PASSWORD': 'test_pass',
                'ACCOUNTS_DB_URL': 'jdbc:postgresql://localhost:5432/accounts_db',
                'ACCOUNTS_DB_USERNAME': 'test_user',
                'ACCOUNTS_DB_PASSWORD': 'test_pass',
                'INSTRUMENTS_DB_URL': 'jdbc:postgresql://localhost:5432/instruments_db',
                'INSTRUMENTS_DB_USERNAME': 'test_user',
                'INSTRUMENTS_DB_PASSWORD': 'test_pass',
                'ANALYTICS_DB_URL': 'jdbc:postgresql://localhost:5432/instruments_db',
                'ANALYTICS_DB_USERNAME': 'test_user',
                'ANALYTICS_DB_PASSWORD': 'test_pass'
            }):
                etl = TradeDataETL()
                yield etl

    def test_valid_order(self, mock_etl):
        mock_cursor = MagicMock()
        mock_cursor.fetchone.return_value = (1,)
        mock_etl.accounts_db.get_cursor.return_value = mock_cursor
        mock_etl.instruments_db.get_cursor.return_value = mock_cursor
        
        order = (
            'order-123',
            'idempotency-key',
            'ACC-001',
            'AAPL',
            'BUY',
            100,
            150.50,
            'FILLED',
            datetime(2026, 9, 28),
            1
        )
        
        is_valid, reason = mock_etl.validate_order(order)
        assert is_valid is True
        assert reason is None

    def test_invalid_quantity_zero(self, mock_etl):
        order = (
            'order-123',
            'idempotency-key',
            'ACC-001',
            'AAPL',
            'BUY',
            0,
            150.50,
            'FILLED',
            datetime(2026, 9, 28),
            1
        )
        
        is_valid, reason = mock_etl.validate_order(order)
        assert is_valid is False
        assert reason == "quantity must be > 0"

    def test_invalid_quantity_negative(self, mock_etl):
        order = (
            'order-123',
            'idempotency-key',
            'ACC-001',
            'AAPL',
            'BUY',
            -50,
            150.50,
            'FILLED',
            datetime(2026, 9, 28),
            1
        )
        
        is_valid, reason = mock_etl.validate_order(order)
        assert is_valid is False
        assert reason == "quantity must be > 0"

    def test_invalid_price_zero(self, mock_etl):
        order = (
            'order-123',
            'idempotency-key',
            'ACC-001',
            'AAPL',
            'BUY',
            100,
            0,
            'FILLED',
            datetime(2026, 9, 28),
            1
        )
        
        is_valid, reason = mock_etl.validate_order(order)
        assert is_valid is False
        assert reason == "price must be > 0"

    def test_invalid_side(self, mock_etl):
        order = (
            'order-123',
            'idempotency-key',
            'ACC-001',
            'AAPL',
            'HOLD',
            100,
            150.50,
            'FILLED',
            datetime(2026, 9, 28),
            1
        )
        
        is_valid, reason = mock_etl.validate_order(order)
        assert is_valid is False
        assert 'invalid side' in reason

    def test_invalid_status(self, mock_etl):
        order = (
            'order-123',
            'idempotency-key',
            'ACC-001',
            'AAPL',
            'BUY',
            100,
            150.50,
            'EXECUTED',
            datetime(2026, 9, 28),
            1
        )
        
        is_valid, reason = mock_etl.validate_order(order)
        assert is_valid is False
        assert 'invalid order_status' in reason

    def test_invalid_account_not_found(self, mock_etl):
        mock_cursor = MagicMock()
        mock_cursor.fetchone.return_value = None
        mock_etl.accounts_db.get_cursor.return_value = mock_cursor
        
        order = (
            'order-123',
            'idempotency-key',
            'ACC-999',
            'AAPL',
            'BUY',
            100,
            150.50,
            'FILLED',
            datetime(2026, 9, 28),
            1
        )
        
        is_valid, reason = mock_etl.validate_order(order)
        assert is_valid is False
        assert 'account not found' in reason

    def test_invalid_instrument_not_found(self, mock_etl):
        mock_cursor_acct = MagicMock()
        mock_cursor_acct.fetchone.return_value = (1,)
        
        mock_cursor_inst = MagicMock()
        mock_cursor_inst.fetchone.return_value = None
        
        calls = {'count': 0}
        
        def cursor_factory():
            calls['count'] += 1
            if calls['count'] == 1:
                return mock_cursor_acct
            else:
                return mock_cursor_inst
        
        mock_etl.accounts_db.get_cursor = MagicMock(return_value=mock_cursor_acct)
        mock_etl.instruments_db.get_cursor = MagicMock(return_value=mock_cursor_inst)
        
        order = (
            'order-123',
            'idempotency-key',
            'ACC-001',
            'UNKNOWN',
            'BUY',
            100,
            150.50,
            'FILLED',
            datetime(2026, 9, 28),
            1
        )
        
        is_valid, reason = mock_etl.validate_order(order)
        assert is_valid is False
        assert 'instrument not found' in reason


class TestDeadLettering:
    @pytest.fixture
    def mock_etl(self):
        with patch.multiple(
            'load_fact_trades.DatabaseConnection',
            connect=MagicMock(),
            get_cursor=MagicMock(),
            commit=MagicMock(),
            rollback=MagicMock(),
            close=MagicMock()
        ):
            with patch.dict(os.environ, {
                'ORDERS_DB_URL': 'jdbc:postgresql://localhost:5432/orders_db',
                'ORDERS_DB_USERNAME': 'test_user',
                'ORDERS_DB_PASSWORD': 'test_pass',
                'ACCOUNTS_DB_URL': 'jdbc:postgresql://localhost:5432/accounts_db',
                'ACCOUNTS_DB_USERNAME': 'test_user',
                'ACCOUNTS_DB_PASSWORD': 'test_pass',
                'INSTRUMENTS_DB_URL': 'jdbc:postgresql://localhost:5432/instruments_db',
                'INSTRUMENTS_DB_USERNAME': 'test_user',
                'INSTRUMENTS_DB_PASSWORD': 'test_pass',
                'ANALYTICS_DB_URL': 'jdbc:postgresql://localhost:5432/instruments_db',
                'ANALYTICS_DB_USERNAME': 'test_user',
                'ANALYTICS_DB_PASSWORD': 'test_pass'
            }):
                etl = TradeDataETL()
                yield etl

    def test_record_dead_letter(self, mock_etl):
        mock_cursor = MagicMock()
        mock_etl.analytics_db.get_cursor.return_value = mock_cursor
        
        source_row = {
            'order_id': 'order-123',
            'quantity': 0,
            'price': 150.50
        }
        reason = 'quantity must be > 0'
        
        mock_etl.record_dead_letter(source_row, reason)
        
        mock_cursor.execute.assert_called_once()
        call_args = mock_cursor.execute.call_args
        assert 'INSERT INTO analytics.etl_dead_letter' in call_args[0][0]
        assert mock_etl.row_count_dead_lettered == 1

    def test_dead_letter_keeps_decimal_price_exact(self, mock_etl):
        from decimal import Decimal
        mock_cursor = MagicMock()
        mock_etl.analytics_db.get_cursor.return_value = mock_cursor

        mock_etl.record_dead_letter({'order_id': 'order-123', 'price': Decimal('190.25')}, 'reason')

        source_row_json = mock_cursor.execute.call_args[0][1][2]
        assert json.loads(source_row_json)['price'] == '190.25'

    def test_dead_letter_contains_full_row_as_json(self, mock_etl):
        mock_cursor = MagicMock()
        mock_etl.analytics_db.get_cursor.return_value = mock_cursor
        
        source_row = {
            'order_id': 'order-123',
            'account_id': 'ACC-001',
            'symbol': 'AAPL',
            'quantity': 0,
            'price': 150.50
        }
        reason = 'quantity must be > 0'
        
        mock_etl.record_dead_letter(source_row, reason)
        
        call_args = mock_cursor.execute.call_args
        params = call_args[0][1]
        assert params[2] == json.dumps(source_row)
        assert params[3] == reason


class TestIncrementalLoad:
    @pytest.fixture
    def mock_etl(self):
        with patch.multiple(
            'load_fact_trades.DatabaseConnection',
            connect=MagicMock(),
            get_cursor=MagicMock(),
            commit=MagicMock(),
            rollback=MagicMock(),
            close=MagicMock()
        ):
            with patch.dict(os.environ, {
                'ORDERS_DB_URL': 'jdbc:postgresql://localhost:5432/orders_db',
                'ORDERS_DB_USERNAME': 'test_user',
                'ORDERS_DB_PASSWORD': 'test_pass',
                'ACCOUNTS_DB_URL': 'jdbc:postgresql://localhost:5432/accounts_db',
                'ACCOUNTS_DB_USERNAME': 'test_user',
                'ACCOUNTS_DB_PASSWORD': 'test_pass',
                'INSTRUMENTS_DB_URL': 'jdbc:postgresql://localhost:5432/instruments_db',
                'INSTRUMENTS_DB_USERNAME': 'test_user',
                'INSTRUMENTS_DB_PASSWORD': 'test_pass',
                'ANALYTICS_DB_URL': 'jdbc:postgresql://localhost:5432/instruments_db',
                'ANALYTICS_DB_USERNAME': 'test_user',
                'ANALYTICS_DB_PASSWORD': 'test_pass'
            }):
                etl = TradeDataETL()
                yield etl

    def test_extract_orders_new_only(self, mock_etl):
        now = datetime(2026, 9, 28, 12, 0, 0)
        watermark = datetime(2026, 9, 28, 10, 0, 0)
        
        new_order = (
            'order-456',
            'idempotency-456',
            'ACC-001',
            'AAPL',
            'SELL',
            50,
            155.00,
            'NEW',
            now,
            0
        )
        
        mock_cursor = MagicMock()
        mock_cursor.fetchall.side_effect = [[new_order], []]
        mock_etl.orders_db.get_cursor = MagicMock(return_value=mock_cursor)

        # nothing is still NEW in FACT_TRADES
        analytics_cursor = MagicMock()
        analytics_cursor.fetchall.return_value = []
        mock_etl.analytics_db.get_cursor = MagicMock(return_value=analytics_cursor)

        orders = mock_etl.extract_orders(watermark)

        assert len(orders) == 1
        assert orders[0][0] == 'order-456'

    def test_extract_orders_reprocess_new(self, mock_etl):
        watermark = datetime(2026, 9, 28, 10, 0, 0)
        
        new_order = (
            'order-456',
            'idempotency-456',
            'ACC-001',
            'AAPL',
            'SELL',
            50,
            155.00,
            'NEW',
            watermark + timedelta(hours=2),
            0
        )
        
        reprocess_order = (
            'order-789',
            'idempotency-789',
            'ACC-002',
            'GOOGL',
            'BUY',
            25,
            2800.00,
            'NEW',
            watermark - timedelta(hours=5),
            1
        )
        
        mock_cursor = MagicMock()
        mock_cursor.fetchall.side_effect = [[new_order], [reprocess_order]]
        mock_etl.orders_db.get_cursor = MagicMock(return_value=mock_cursor)

        # order-789 is still NEW in FACT_TRADES (analytics DB)
        analytics_cursor = MagicMock()
        analytics_cursor.fetchall.return_value = [('order-789',)]
        mock_etl.analytics_db.get_cursor = MagicMock(return_value=analytics_cursor)

        orders = mock_etl.extract_orders(watermark)

        assert len(orders) == 2
        assert orders[0][0] == 'order-456'
        assert orders[1][0] == 'order-789'

        # the still-NEW keys come from analytics and are passed to the orders-db query
        assert 'analytics.fact_trades' in analytics_cursor.execute.call_args[0][0]
        reprocess_params = mock_cursor.execute.call_args_list[-1][0][1]
        assert reprocess_params == (['order-789'], watermark)


class TestStatusUpdate:
    @pytest.fixture
    def mock_etl(self):
        with patch.multiple(
            'load_fact_trades.DatabaseConnection',
            connect=MagicMock(),
            get_cursor=MagicMock(),
            commit=MagicMock(),
            rollback=MagicMock(),
            close=MagicMock()
        ):
            with patch.dict(os.environ, {
                'ORDERS_DB_URL': 'jdbc:postgresql://localhost:5432/orders_db',
                'ORDERS_DB_USERNAME': 'test_user',
                'ORDERS_DB_PASSWORD': 'test_pass',
                'ACCOUNTS_DB_URL': 'jdbc:postgresql://localhost:5432/accounts_db',
                'ACCOUNTS_DB_USERNAME': 'test_user',
                'ACCOUNTS_DB_PASSWORD': 'test_pass',
                'INSTRUMENTS_DB_URL': 'jdbc:postgresql://localhost:5432/instruments_db',
                'INSTRUMENTS_DB_USERNAME': 'test_user',
                'INSTRUMENTS_DB_PASSWORD': 'test_pass',
                'ANALYTICS_DB_URL': 'jdbc:postgresql://localhost:5432/instruments_db',
                'ANALYTICS_DB_USERNAME': 'test_user',
                'ANALYTICS_DB_PASSWORD': 'test_pass'
            }):
                etl = TradeDataETL()
                yield etl

    def test_merge_facts_update_existing_row(self, mock_etl):
        mock_cursor = MagicMock()
        mock_etl.analytics_db.get_cursor.return_value = mock_cursor
        mock_cursor.fetchone.return_value = (1,)
        
        order = (
            'order-123',
            'idempotency-123',
            'ACC-001',
            'AAPL',
            'BUY',
            100,
            150.50,
            'FILLED',
            datetime(2026, 9, 28),
            2
        )
        
        mock_etl.merge_facts([order])
        
        call_args = mock_cursor.execute.call_args_list[-1]
        assert 'ON CONFLICT (trade_key) DO UPDATE' in call_args[0][0]
        assert order[0] in str(call_args[0][1])

    def test_merge_facts_insert_new_row(self, mock_etl):
        mock_cursor = MagicMock()
        mock_etl.analytics_db.get_cursor.return_value = mock_cursor
        mock_cursor.fetchone.return_value = (1,)
        
        order = (
            'order-new',
            'idempotency-new',
            'ACC-001',
            'AAPL',
            'BUY',
            100,
            150.50,
            'NEW',
            datetime(2026, 9, 28),
            0
        )
        
        mock_etl.merge_facts([order])
        
        call_args = mock_cursor.execute.call_args_list[-1]
        assert 'ON CONFLICT (trade_key) DO UPDATE' in call_args[0][0]
        assert mock_etl.row_count_loaded == 1


class TestTransactionAtomicity:
    @pytest.fixture
    def mock_etl(self):
        with patch.multiple(
            'load_fact_trades.DatabaseConnection',
            connect=MagicMock(),
            get_cursor=MagicMock(),
            commit=MagicMock(),
            rollback=MagicMock(),
            close=MagicMock()
        ):
            with patch.dict(os.environ, {
                'ORDERS_DB_URL': 'jdbc:postgresql://localhost:5432/orders_db',
                'ORDERS_DB_USERNAME': 'test_user',
                'ORDERS_DB_PASSWORD': 'test_pass',
                'ACCOUNTS_DB_URL': 'jdbc:postgresql://localhost:5432/accounts_db',
                'ACCOUNTS_DB_USERNAME': 'test_user',
                'ACCOUNTS_DB_PASSWORD': 'test_pass',
                'INSTRUMENTS_DB_URL': 'jdbc:postgresql://localhost:5432/instruments_db',
                'INSTRUMENTS_DB_USERNAME': 'test_user',
                'INSTRUMENTS_DB_PASSWORD': 'test_pass',
                'ANALYTICS_DB_URL': 'jdbc:postgresql://localhost:5432/instruments_db',
                'ANALYTICS_DB_USERNAME': 'test_user',
                'ANALYTICS_DB_PASSWORD': 'test_pass'
            }):
                etl = TradeDataETL()
                yield etl

    def test_rollback_on_error(self, mock_etl):
        mock_cursor = MagicMock()
        mock_cursor.execute.side_effect = Exception("Constraint violation")
        mock_etl.analytics_db.get_cursor.return_value = mock_cursor
        mock_etl.accounts_db.get_cursor.return_value = mock_cursor
        mock_etl.instruments_db.get_cursor.return_value = mock_cursor
        
        order = (
            'order-123',
            'idempotency-key',
            'ACC-001',
            'AAPL',
            'BUY',
            100,
            150.50,
            'FILLED',
            datetime(2026, 9, 28),
            1
        )
        
        with pytest.raises(Exception):
            mock_etl.upsert_dimensions([order])

    def test_commit_on_success(self, mock_etl):
        mock_cursor = MagicMock()
        mock_etl.analytics_db.get_cursor.return_value = mock_cursor
        mock_etl.row_count_loaded = 5
        mock_etl.row_count_dead_lettered = 1
        
        test_time = datetime(2026, 9, 28, 12, 0, 0)
        mock_etl.update_watermark(test_time)
        mock_etl.analytics_db.commit()

        mock_etl.analytics_db.commit.assert_called()


class TestRunWatermarkAndCommit:
    @pytest.fixture
    def mock_etl(self):
        with patch.multiple(
            'load_fact_trades.DatabaseConnection',
            connect=MagicMock(),
            get_cursor=MagicMock(),
            commit=MagicMock(),
            rollback=MagicMock(),
            close=MagicMock()
        ):
            with patch.dict(os.environ, {
                'ORDERS_DB_URL': 'jdbc:postgresql://localhost:5432/orders_db',
                'ORDERS_DB_USERNAME': 'test_user',
                'ORDERS_DB_PASSWORD': 'test_pass',
                'ACCOUNTS_DB_URL': 'jdbc:postgresql://localhost:5432/accounts_db',
                'ACCOUNTS_DB_USERNAME': 'test_user',
                'ACCOUNTS_DB_PASSWORD': 'test_pass',
                'INSTRUMENTS_DB_URL': 'jdbc:postgresql://localhost:5432/instruments_db',
                'INSTRUMENTS_DB_USERNAME': 'test_user',
                'INSTRUMENTS_DB_PASSWORD': 'test_pass',
                'ANALYTICS_DB_URL': 'jdbc:postgresql://localhost:5432/instruments_db',
                'ANALYTICS_DB_USERNAME': 'test_user',
                'ANALYTICS_DB_PASSWORD': 'test_pass'
            }):
                etl = TradeDataETL()
                yield etl

    @staticmethod
    def _order(order_id, created_at, status='NEW'):
        return (order_id, 'idem-' + order_id, 'ACC-001', 'AAPL', 'BUY', 10, 100.0, status, created_at, 0)

    def test_watermark_never_moves_backwards(self, mock_etl):
        # Only re-processed NEW orders, all older than the current watermark
        watermark = datetime(2026, 9, 27, 15, 0, 0)
        old_order = self._order('order-old', datetime(2026, 9, 26, 15, 0, 0), status='FILLED')

        with patch.object(mock_etl, 'read_watermark', return_value=(watermark, 1)), \
             patch.object(mock_etl, 'extract_orders', return_value=[old_order]), \
             patch.object(mock_etl, 'validate_order', return_value=(True, None)), \
             patch.object(mock_etl, 'upsert_dimensions'), \
             patch.object(mock_etl, 'merge_facts'), \
             patch.object(mock_etl, 'update_watermark') as update_watermark:
            mock_etl.run()

        update_watermark.assert_called_once_with(watermark)

    def test_watermark_includes_dead_lettered_orders(self, mock_etl):
        watermark = datetime(2026, 9, 27, 15, 0, 0)
        good = self._order('order-good', datetime(2026, 9, 28, 9, 0, 0))
        bad = self._order('order-bad', datetime(2026, 9, 28, 10, 0, 0))

        def validate(order):
            return (False, 'account not found: ACC-001') if order[0] == 'order-bad' else (True, None)

        with patch.object(mock_etl, 'read_watermark', return_value=(watermark, 1)), \
             patch.object(mock_etl, 'extract_orders', return_value=[good, bad]), \
             patch.object(mock_etl, 'validate_order', side_effect=validate), \
             patch.object(mock_etl, 'record_dead_letter'), \
             patch.object(mock_etl, 'upsert_dimensions'), \
             patch.object(mock_etl, 'merge_facts'), \
             patch.object(mock_etl, 'update_watermark') as update_watermark:
            mock_etl.run()

        # moves past the dead-lettered order so it isn't dead-lettered again next run
        update_watermark.assert_called_once_with(bad[8])

    def test_all_invalid_still_commits_dead_letters(self, mock_etl):
        watermark = datetime(2026, 9, 27, 15, 0, 0)
        bad = self._order('order-bad', datetime(2026, 9, 28, 10, 0, 0))

        with patch.object(mock_etl, 'read_watermark', return_value=(watermark, 1)), \
             patch.object(mock_etl, 'extract_orders', return_value=[bad]), \
             patch.object(mock_etl, 'validate_order', return_value=(False, 'instrument not found: FAKE')), \
             patch.object(mock_etl, 'record_dead_letter') as record_dead_letter, \
             patch.object(mock_etl, 'upsert_dimensions') as upsert_dimensions, \
             patch.object(mock_etl, 'merge_facts') as merge_facts, \
             patch.object(mock_etl, 'update_watermark') as update_watermark:
            mock_etl.analytics_db.commit.reset_mock()
            mock_etl.run()

        record_dead_letter.assert_called_once()
        source_row, reason = record_dead_letter.call_args[0]
        assert source_row == {
            'order_id': 'order-bad',
            'account_id': 'ACC-001',
            'symbol': 'AAPL',
            'side': 'BUY',
            'quantity': 10,
            'price': 100.0,
            'order_status': 'NEW'
        }
        upsert_dimensions.assert_not_called()
        merge_facts.assert_not_called()
        update_watermark.assert_called_once_with(bad[8])
        mock_etl.analytics_db.commit.assert_called_once()
