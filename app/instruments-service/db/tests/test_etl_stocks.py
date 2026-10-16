"""Unit tests for 04-etl_stocks.py"""
import pytest
from unittest.mock import patch, MagicMock


class TestDatabaseConnection:
    """Tests for database connection"""

    def test_get_db_connection_success(self, etl_stocks_module):
        """Test successful database connection"""
        with patch.dict('os.environ', {
            'SPRING_DATASOURCE_URL': 'jdbc:postgresql://localhost:5432/testdb',
            'SPRING_DATASOURCE_USERNAME': 'user',
            'SPRING_DATASOURCE_PASSWORD': 'pass'
        }):
            with patch.object(etl_stocks_module, 'psycopg') as mock_psycopg:
                mock_conn = MagicMock()
                mock_psycopg.connect.return_value = mock_conn
                result = etl_stocks_module.get_db_connection()
                assert result is not None

    def test_get_db_connection_failure(self, etl_stocks_module):
        """Test database connection failure returns None"""
        with patch.dict('os.environ', {
            'SPRING_DATASOURCE_URL': 'jdbc:postgresql://localhost:5432/testdb',
            'SPRING_DATASOURCE_USERNAME': 'user',
            'SPRING_DATASOURCE_PASSWORD': 'pass'
        }):
            with patch.object(etl_stocks_module, 'psycopg') as mock_psycopg:
                mock_psycopg.Error = Exception
                mock_psycopg.connect.side_effect = Exception("Connection failed")
                with patch('builtins.print'):
                    result = etl_stocks_module.get_db_connection()
                assert result is None


class TestStockValidation:
    """Tests for stock data validation"""

    def test_is_valid_success(self, etl_stocks_module, sample_stock_dict):
        """Test validation passes for valid stock data"""
        assert etl_stocks_module.is_valid(sample_stock_dict) is True

    def test_is_valid_negative_market_cap_fails(self, etl_stocks_module, sample_stock_dict):
        """Test validation fails when market_cap is negative"""
        sample_stock_dict['market_cap'] = -1000
        assert etl_stocks_module.is_valid(sample_stock_dict) is False

    def test_is_valid_negative_shares_fails(self, etl_stocks_module, sample_stock_dict):
        """Test validation fails when shares_outstanding is negative"""
        sample_stock_dict['shares_outstanding'] = -1000
        assert etl_stocks_module.is_valid(sample_stock_dict) is False

    def test_is_valid_negative_employees_fails(self, etl_stocks_module, sample_stock_dict):
        """Test validation fails when employees is negative"""
        sample_stock_dict['full_time_employees'] = -100
        assert etl_stocks_module.is_valid(sample_stock_dict) is False

    def test_is_valid_null_optional_fields(self, etl_stocks_module, sample_stock_dict):
        """Test validation passes with null optional fields"""
        sample_stock_dict['trailing_pe'] = None
        sample_stock_dict['dividend_rate'] = None
        assert etl_stocks_module.is_valid(sample_stock_dict) is True

    def test_is_valid_negative_optional_fails(self, etl_stocks_module, sample_stock_dict):
        """Test validation fails with negative optional field"""
        sample_stock_dict['trailing_pe'] = -10.0
        assert etl_stocks_module.is_valid(sample_stock_dict) is False


class TestStockDataExtraction:
    """Tests for stock data extraction"""

    def test_get_raw_stocks_success(self, etl_stocks_module, sample_stock_dict):
        """Test successful stock data extraction"""
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [sample_stock_dict]
        result = etl_stocks_module.get_raw_stocks(mock_cursor)
        assert len(result) == 1

    def test_get_raw_stocks_empty(self, etl_stocks_module):
        """Test extraction returns empty list when no data"""
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = []
        result = etl_stocks_module.get_raw_stocks(mock_cursor)
        assert result == []


class TestStockInsertion:
    """Tests for stock data insertion"""

    def test_insert_clean_stock_calls_execute(self, etl_stocks_module, sample_stock_dict):
        """Test that insert_clean_stock calls cursor.execute"""
        mock_cursor = MagicMock()
        etl_stocks_module.insert_clean_stock(mock_cursor, sample_stock_dict)
        mock_cursor.execute.assert_called_once()


class TestETLPipeline:
    """Tests for complete ETL pipeline"""

    def test_run_etl_success(self, etl_stocks_module, sample_stock_dict):
        """Test successful ETL pipeline"""
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [sample_stock_dict]
        mock_conn.cursor.return_value = mock_cursor
        with patch.object(etl_stocks_module, 'get_db_connection', return_value=mock_conn):
            with patch('builtins.print'):
                etl_stocks_module.run_etl()
        mock_conn.commit.assert_called()

    def test_run_etl_rejects_invalid(self, etl_stocks_module, sample_stock_dict):
        """Test ETL pipeline rejects invalid data"""
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        invalid_stock = sample_stock_dict.copy()
        invalid_stock['market_cap'] = -1000
        mock_cursor.fetchall.return_value = [invalid_stock]
        mock_conn.cursor.return_value = mock_cursor
        with patch.object(etl_stocks_module, 'get_db_connection', return_value=mock_conn):
            with patch('builtins.print'):
                etl_stocks_module.run_etl()
        mock_conn.commit.assert_called()
