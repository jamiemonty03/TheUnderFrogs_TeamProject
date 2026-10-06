"""Unit tests for 06-etl_bonds.py"""
import pytest
from unittest.mock import patch, MagicMock


class TestDatabaseConnection:
    """Tests for database connection"""

    def test_get_db_connection_success(self, etl_bonds_module):
        """Test successful database connection"""
        with patch.dict('os.environ', {
            'SPRING_DATASOURCE_URL': 'jdbc:postgresql://localhost:5432/testdb',
            'SPRING_DATASOURCE_USERNAME': 'user',
            'SPRING_DATASOURCE_PASSWORD': 'pass'
        }):
            with patch.object(etl_bonds_module, 'psycopg') as mock_psycopg:
                mock_conn = MagicMock()
                mock_psycopg.connect.return_value = mock_conn
                result = etl_bonds_module.get_db_connection()
                assert result is not None

    def test_get_db_connection_failure(self, etl_bonds_module):
        """Test database connection failure returns None"""
        with patch.dict('os.environ', {
            'SPRING_DATASOURCE_URL': 'jdbc:postgresql://localhost:5432/testdb',
            'SPRING_DATASOURCE_USERNAME': 'user',
            'SPRING_DATASOURCE_PASSWORD': 'pass'
        }):
            with patch.object(etl_bonds_module, 'psycopg') as mock_psycopg:
                mock_psycopg.Error = Exception
                mock_psycopg.connect.side_effect = Exception("Connection failed")
                with patch('builtins.print'):
                    result = etl_bonds_module.get_db_connection()
                assert result is None


class TestBondValidation:
    """Tests for bond data validation"""

    def test_is_valid_success(self, etl_bonds_module, sample_bond_dict):
        """Test validation passes for valid bond data"""
        assert etl_bonds_module.is_valid(sample_bond_dict) is True

    def test_is_valid_zero_nav_price_fails(self, etl_bonds_module, sample_bond_dict):
        """Test validation fails when nav_price is zero"""
        sample_bond_dict['nav_price'] = 0
        assert etl_bonds_module.is_valid(sample_bond_dict) is False

    def test_is_valid_negative_nav_price_fails(self, etl_bonds_module, sample_bond_dict):
        """Test validation fails when nav_price is negative"""
        sample_bond_dict['nav_price'] = -100.0
        assert etl_bonds_module.is_valid(sample_bond_dict) is False

    def test_is_valid_negative_total_assets_fails(self, etl_bonds_module, sample_bond_dict):
        """Test validation fails when total_assets is negative"""
        sample_bond_dict['total_assets'] = -1000000
        assert etl_bonds_module.is_valid(sample_bond_dict) is False

    def test_is_valid_negative_net_assets_fails(self, etl_bonds_module, sample_bond_dict):
        """Test validation fails when net_assets is negative"""
        sample_bond_dict['net_assets'] = -1000000
        assert etl_bonds_module.is_valid(sample_bond_dict) is False

    def test_is_valid_null_optional_fields(self, etl_bonds_module, sample_bond_dict):
        """Test validation passes with null optional fields"""
        sample_bond_dict['net_assets'] = None
        sample_bond_dict['net_expense_ratio'] = None
        assert etl_bonds_module.is_valid(sample_bond_dict) is True

    def test_is_valid_negative_distribution_yield_fails(self, etl_bonds_module, sample_bond_dict):
        """Test validation fails with negative distribution yield"""
        sample_bond_dict['distribution_yield'] = -1.0
        assert etl_bonds_module.is_valid(sample_bond_dict) is False


class TestBondDataExtraction:
    """Tests for bond data extraction"""

    def test_get_raw_bonds_success(self, etl_bonds_module, sample_bond_dict):
        """Test successful bond data extraction"""
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [sample_bond_dict]
        result = etl_bonds_module.get_raw_bonds(mock_cursor)
        assert len(result) == 1

    def test_get_raw_bonds_empty(self, etl_bonds_module):
        """Test extraction returns empty list when no data"""
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = []
        result = etl_bonds_module.get_raw_bonds(mock_cursor)
        assert result == []


class TestBondInsertion:
    """Tests for bond data insertion"""

    def test_insert_clean_bond_calls_execute(self, etl_bonds_module, sample_bond_dict):
        """Test that insert_clean_bond calls cursor.execute"""
        mock_cursor = MagicMock()
        etl_bonds_module.insert_clean_bond(mock_cursor, sample_bond_dict)
        mock_cursor.execute.assert_called_once()


class TestETLPipeline:
    """Tests for complete ETL pipeline"""

    def test_run_etl_success(self, etl_bonds_module, sample_bond_dict):
        """Test successful ETL pipeline"""
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = [sample_bond_dict]
        mock_conn.cursor.return_value = mock_cursor
        with patch.object(etl_bonds_module, 'get_db_connection', return_value=mock_conn):
            with patch('builtins.print'):
                etl_bonds_module.run_etl()
        mock_conn.commit.assert_called()

    def test_run_etl_rejects_invalid(self, etl_bonds_module, sample_bond_dict):
        """Test ETL pipeline rejects invalid data"""
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        invalid_bond = sample_bond_dict.copy()
        invalid_bond['nav_price'] = 0
        mock_cursor.fetchall.return_value = [invalid_bond]
        mock_conn.cursor.return_value = mock_cursor
        with patch.object(etl_bonds_module, 'get_db_connection', return_value=mock_conn):
            with patch('builtins.print'):
                etl_bonds_module.run_etl()
        mock_conn.commit.assert_called()
