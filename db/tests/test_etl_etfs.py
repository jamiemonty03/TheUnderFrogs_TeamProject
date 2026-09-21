"""Unit tests for 05-etl_etfs.py"""
import pytest
from unittest.mock import patch, MagicMock


class TestDatabaseConnection:
    """Tests for database connection"""

    def test_get_db_connection_success(self, etl_etfs_module):
        """Test successful database connection"""
        with patch.dict('os.environ', {
            'DB_HOST': 'localhost',
            'DB_PORT': '5432',
            'POSTGRES_DB': 'testdb',
            'POSTGRES_USER': 'user',
            'POSTGRES_PASSWORD': 'pass'
        }):
            with patch.object(etl_etfs_module, 'psycopg') as mock_psycopg:
                mock_conn = MagicMock()
                mock_psycopg.connect.return_value = mock_conn
                result = etl_etfs_module.get_db_connection()
                assert result is not None

    def test_get_db_connection_failure(self, etl_etfs_module):
        """Test database connection failure returns None"""
        with patch.dict('os.environ', {
            'DB_HOST': 'localhost',
            'DB_PORT': '5432',
            'POSTGRES_DB': 'testdb',
            'POSTGRES_USER': 'user',
            'POSTGRES_PASSWORD': 'pass'
        }):
            with patch.object(etl_etfs_module, 'psycopg') as mock_psycopg:
                mock_psycopg.Error = Exception
                mock_psycopg.connect.side_effect = Exception("Connection failed")
                with patch('builtins.print'):
                    result = etl_etfs_module.get_db_connection()
                assert result is None


class TestETFValidation:
    """Tests for ETF data validation"""

    def test_is_valid_success(self, etl_etfs_module, sample_etf_dict):
        """Test validation passes for valid ETF data"""
        assert etl_etfs_module.is_valid(sample_etf_dict) is True

    def test_is_valid_zero_nav_price_fails(self, etl_etfs_module, sample_etf_dict):
        """Test validation fails when nav_price is zero"""
        sample_etf_dict['nav_price'] = 0
        assert etl_etfs_module.is_valid(sample_etf_dict) is False

    def test_is_valid_negative_nav_price_fails(self, etl_etfs_module, sample_etf_dict):
        """Test validation fails when nav_price is negative"""
        sample_etf_dict['nav_price'] = -100.0
        assert etl_etfs_module.is_valid(sample_etf_dict) is False

    def test_is_valid_zero_total_assets_fails(self, etl_etfs_module, sample_etf_dict):
        """Test validation fails when total_assets is zero"""
        sample_etf_dict['total_assets'] = 0
        assert etl_etfs_module.is_valid(sample_etf_dict) is False

    def test_is_valid_negative_total_assets_fails(self, etl_etfs_module, sample_etf_dict):
        """Test validation fails when total_assets is negative"""
        sample_etf_dict['total_assets'] = -1000000
        assert etl_etfs_module.is_valid(sample_etf_dict) is False

    def test_is_valid_zero_net_assets_fails(self, etl_etfs_module, sample_etf_dict):
        """Test validation fails when net_assets is zero"""
        sample_etf_dict['net_assets'] = 0
        assert etl_etfs_module.is_valid(sample_etf_dict) is False

    def test_is_valid_null_optional_fields(self, etl_etfs_module, sample_etf_dict):
        """Test validation passes with null optional fields"""
        sample_etf_dict['net_assets'] = None
        sample_etf_dict['net_expense_ratio'] = None
        assert etl_etfs_module.is_valid(sample_etf_dict) is True

    def test_is_valid_negative_expense_ratio_fails(self, etl_etfs_module, sample_etf_dict):
        """Test validation fails with negative expense ratio"""
        sample_etf_dict['net_expense_ratio'] = -0.5
        assert etl_etfs_module.is_valid(sample_etf_dict) is False


class TestETFDataExtraction:
    """Tests for ETF data extraction"""

    def test_get_raw_etfs_success(self, etl_etfs_module, sample_etf_dict):
        """Test successful ETF data extraction"""
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [sample_etf_dict]
        result = etl_etfs_module.get_raw_etfs(mock_cursor)
        assert len(result) == 1

    def test_get_raw_etfs_empty(self, etl_etfs_module):
        """Test extraction returns empty list when no data"""
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = []
        result = etl_etfs_module.get_raw_etfs(mock_cursor)
        assert result == []


class TestETFInsertion:
    """Tests for ETF data insertion"""

    def test_insert_clean_etf_calls_execute(self, etl_etfs_module, sample_etf_dict):
        """Test that insert_clean_etf calls cursor.execute"""
        mock_cursor = MagicMock()
        etl_etfs_module.insert_clean_etf(mock_cursor, sample_etf_dict)
        mock_cursor.execute.assert_called_once()


class TestETLPipeline:
    """Tests for complete ETL pipeline"""

    def test_run_etl_success(self, etl_etfs_module, sample_etf_dict):
        """Test successful ETL pipeline"""
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [sample_etf_dict]
        mock_conn.cursor.return_value = mock_cursor
        with patch.object(etl_etfs_module, 'get_db_connection', return_value=mock_conn):
            with patch('builtins.print'):
                etl_etfs_module.run_etl()
        mock_conn.commit.assert_called()

    def test_run_etl_rejects_invalid(self, etl_etfs_module, sample_etf_dict):
        """Test ETL pipeline rejects invalid data"""
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        invalid_etf = sample_etf_dict.copy()
        invalid_etf['nav_price'] = 0
        mock_cursor.fetchall.return_value = [invalid_etf]
        mock_conn.cursor.return_value = mock_cursor
        with patch.object(etl_etfs_module, 'get_db_connection', return_value=mock_conn):
            with patch('builtins.print'):
                etl_etfs_module.run_etl()
        mock_conn.commit.assert_called()
