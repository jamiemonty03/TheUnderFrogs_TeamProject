"""Unit tests for 03-etl_prices.py"""
import pytest
from unittest.mock import patch, MagicMock
from datetime import date


class TestDatabaseConnection:
    """Tests for database connection"""

    def test_get_db_connection_success(self, etl_prices_module):
        """Test successful database connection"""
        with patch.dict('os.environ', {
            'SPRING_DATASOURCE_URL': 'jdbc:postgresql://localhost:5432/testdb',
            'SPRING_DATASOURCE_USERNAME': 'user',
            'SPRING_DATASOURCE_PASSWORD': 'pass'
        }):
            with patch.object(etl_prices_module, 'psycopg') as mock_psycopg:
                mock_conn = MagicMock()
                mock_psycopg.connect.return_value = mock_conn
                result = etl_prices_module.get_db_connection()
                assert result is not None
                mock_psycopg.connect.assert_called_once()

    def test_get_db_connection_failure(self, etl_prices_module):
        """Test database connection failure returns None"""
        with patch.dict('os.environ', {
            'SPRING_DATASOURCE_URL': 'jdbc:postgresql://localhost:5432/testdb',
            'SPRING_DATASOURCE_USERNAME': 'user',
            'SPRING_DATASOURCE_PASSWORD': 'pass'
        }):
            with patch.object(etl_prices_module, 'psycopg') as mock_psycopg:
                mock_psycopg.Error = Exception
                mock_psycopg.connect.side_effect = Exception("Connection failed")
                with patch('builtins.print'):
                    result = etl_prices_module.get_db_connection()
                assert result is None


class TestPriceValidation:
    """Tests for price data validation"""

    def test_is_valid_success(self, etl_prices_module, sample_price_row):
        """Test validation passes for valid price data"""
        assert etl_prices_module.is_valid(sample_price_row) is True

    def test_is_valid_negative_open_fails(self, etl_prices_module):
        """Test validation fails with negative open"""
        row = ('AAPL', date(2024, 1, 1), -150.0, 155.0, 145.0, 152.0, 1000000)
        assert etl_prices_module.is_valid(row) is False

    def test_is_valid_zero_prices_fail(self, etl_prices_module):
        """Test validation fails with any zero prices"""
        row = ('AAPL', date(2024, 1, 1), 0.0, 105.0, 95.0, 102.0, 1000000)
        assert etl_prices_module.is_valid(row) is False

    def test_is_valid_negative_volume_fails(self, etl_prices_module):
        """Test validation fails with negative volume"""
        row = ('AAPL', date(2024, 1, 1), 150.0, 155.0, 145.0, 152.0, -100)
        assert etl_prices_module.is_valid(row) is False

    def test_is_valid_high_less_than_low_fails(self, etl_prices_module):
        """Test validation fails when high < low"""
        row = ('AAPL', date(2024, 1, 1), 150.0, 140.0, 145.0, 152.0, 1000000)
        assert etl_prices_module.is_valid(row) is False

    def test_is_valid_high_less_than_open_fails(self, etl_prices_module):
        """Test validation fails when high < open"""
        row = ('AAPL', date(2024, 1, 1), 160.0, 155.0, 145.0, 152.0, 1000000)
        assert etl_prices_module.is_valid(row) is False

    def test_is_valid_low_greater_than_open_fails(self, etl_prices_module):
        """Test validation fails when low > open"""
        row = ('AAPL', date(2024, 1, 1), 150.0, 155.0, 151.0, 152.0, 1000000)
        assert etl_prices_module.is_valid(row) is False


class TestPriceDataExtraction:
    """Tests for price data extraction"""

    def test_get_raw_prices_success(self, etl_prices_module, sample_price_row):
        """Test successful price data extraction"""
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [sample_price_row]
        mock_conn.cursor.return_value = mock_cursor
        with patch.object(etl_prices_module, 'get_db_connection', return_value=mock_conn):
            result = etl_prices_module.get_raw_prices()
        assert len(result) == 1

    def test_get_raw_prices_empty(self, etl_prices_module):
        """Test extraction returns empty list when no data"""
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = []
        mock_conn.cursor.return_value = mock_cursor
        with patch.object(etl_prices_module, 'get_db_connection', return_value=mock_conn):
            result = etl_prices_module.get_raw_prices()
        assert result == []


class TestPriceInsertion:
    """Tests for price data insertion"""

    def test_insert_clean_price_calls_execute(self, etl_prices_module, sample_price_row):
        """Test that insert_clean_price calls cursor.execute"""
        mock_cursor = MagicMock()
        etl_prices_module.insert_clean_price(mock_cursor, sample_price_row)
        mock_cursor.execute.assert_called_once()


class TestETLPipeline:
    """Tests for complete ETL pipeline"""

    def test_run_etl_success(self, etl_prices_module, sample_price_row):
        """Test successful ETL pipeline"""
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [sample_price_row]
        mock_conn.cursor.return_value = mock_cursor
        with patch.object(etl_prices_module, 'get_db_connection', return_value=mock_conn):
            with patch('builtins.print'):
                etl_prices_module.run_etl()
        mock_conn.commit.assert_called()

    def test_run_etl_no_data(self, etl_prices_module):
        """Test ETL pipeline with no data - returns early"""
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = []
        mock_conn.cursor.return_value = mock_cursor
        with patch.object(etl_prices_module, 'get_db_connection', return_value=mock_conn):
            with patch('builtins.print') as mock_print:
                etl_prices_module.run_etl()
        # Should print "No new raw prices to process." and return early
        mock_print.assert_called()
        # commit() should NOT be called when no data
        mock_conn.commit.assert_not_called()
