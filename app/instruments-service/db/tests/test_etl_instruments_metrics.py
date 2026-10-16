"""Unit tests for etl_instruments_metrics.py"""
import pytest
from unittest.mock import patch, MagicMock
import pandas as pd
from datetime import datetime, timezone, timedelta
import sys
import os

# Add sql directory to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'etl'))

from etl_instruments_metrics import (
    DatabaseConnection,
    extract_price_data,
    calculate_metrics,
    validate_metrics,
    load_to_database,
    run_etl_pipeline
)


@pytest.fixture
def mock_price_data_df():
    """Fixture: Mock price data with instrument info"""
    today = datetime.now(timezone.utc).date()
    return pd.DataFrame({
        'symbol': ['AAPL', 'AAPL', 'AAPL', 'GOOGL', 'GOOGL', 'GOOGL'],
        'date': [
            today - timedelta(days=365),
            today - timedelta(days=180),
            today,
            today - timedelta(days=365),
            today - timedelta(days=180),
            today
        ],
        'close': [120.0, 140.0, 150.0, 110.0, 130.0, 140.0],
        'asset_class': ['EQUITY', 'EQUITY', 'EQUITY', 'EQUITY', 'EQUITY', 'EQUITY'],
        'currency': ['USD', 'USD', 'USD', 'USD', 'USD', 'USD'],
        'exchange': ['NASDAQ', 'NASDAQ', 'NASDAQ', 'NASDAQ', 'NASDAQ', 'NASDAQ']
    })


@pytest.fixture
def mock_db_connection():
    """Fixture: Mock database connection"""
    with patch('etl_instruments_metrics.DatabaseConnection') as mock:
        yield mock


class TestDataExtraction:
    """Tests for data extraction"""

    @patch('etl_instruments_metrics.DatabaseConnection')
    def test_extract_price_data_success(self, mock_db_class, mock_price_data_df):
        """Test successful price data extraction"""
        mock_cursor = MagicMock()
        mock_cursor.description = [('symbol',), ('date',), ('close',), ('asset_class',), 
                                   ('currency',), ('exchange',)]
        mock_cursor.fetchall.return_value = mock_price_data_df.values.tolist()
        
        mock_db = MagicMock()
        mock_db.get_cursor.return_value.__enter__.return_value = mock_cursor
        
        result = extract_price_data(mock_db)
        
        assert isinstance(result, pd.DataFrame)
        assert len(result) == 6
        assert set(result.columns) >= {'symbol', 'date', 'close', 'asset_class', 'currency'}

    @patch('etl_instruments_metrics.DatabaseConnection')
    def test_extract_price_data_empty(self, mock_db_class):
        """Test extraction returns empty DataFrame when no data"""
        mock_cursor = MagicMock()
        mock_cursor.description = [('symbol',)]
        mock_cursor.fetchall.return_value = []
        
        mock_db = MagicMock()
        mock_db.get_cursor.return_value.__enter__.return_value = mock_cursor
        
        result = extract_price_data(mock_db)
        
        assert isinstance(result, pd.DataFrame)
        assert len(result) == 0


class TestMetricsCalculation:
    """Tests for metrics calculation"""

    def test_calculate_metrics_success(self, mock_price_data_df):
        """Test successful metrics calculation"""
        result = calculate_metrics(mock_price_data_df)
        
        assert isinstance(result, pd.DataFrame)
        assert len(result) == 2  # AAPL and GOOGL
        assert set(result.columns) >= {
            'symbol', 'latest_price', 'latest_date', 'price_52w_high', 'price_52w_low',
            'ytd_return', 'one_year_return', 'max_drawdown', 'version', 'created_at', 'updated_by'
        }

    def test_calculate_metrics_audit_columns(self, mock_price_data_df):
        """Test that audit columns are added during calculation"""
        result = calculate_metrics(mock_price_data_df)
        
        # Check audit columns
        assert (result['version'] == 0).all()
        assert result['created_at'].notna().all()
        assert result['updated_by'].eq('ETL_PROCESS').all()

    def test_calculate_metrics_52w_high_low(self, mock_price_data_df):
        """Test 52-week high/low calculation"""
        result = calculate_metrics(mock_price_data_df)
        
        # AAPL prices: 120, 140, 150 -> high=150, low=120
        aapl = result[result['symbol'] == 'AAPL'].iloc[0]
        assert aapl['price_52w_high'] == 150.0
        assert aapl['price_52w_low'] == 120.0

    def test_calculate_metrics_latest_price(self, mock_price_data_df):
        """Test latest price extraction"""
        result = calculate_metrics(mock_price_data_df)
        
        # Latest close prices
        aapl = result[result['symbol'] == 'AAPL'].iloc[0]
        googl = result[result['symbol'] == 'GOOGL'].iloc[0]
        
        assert aapl['latest_price'] == 150.0
        assert googl['latest_price'] == 140.0

    def test_calculate_metrics_one_year_return(self, mock_price_data_df):
        """Test one-year return calculation"""
        result = calculate_metrics(mock_price_data_df)
        
        # AAPL: (150-120)/120 * 100 = 25%
        aapl = result[result['symbol'] == 'AAPL'].iloc[0]
        assert abs(aapl['one_year_return'] - 25.0) < 0.1

    def test_calculate_metrics_max_drawdown(self, mock_price_data_df):
        """Test max drawdown calculation"""
        result = calculate_metrics(mock_price_data_df)
        
        # Max drawdown should be negative (peak to trough)
        assert (result['max_drawdown'] <= 0).all()

    def test_calculate_metrics_days_since_update(self, mock_price_data_df):
        """Test days since update calculation"""
        result = calculate_metrics(mock_price_data_df)
        
        # All should be 0 since latest_date is today
        assert (result['days_since_update'] == 0).all()


    def test_calculate_metrics_null_prices_removed(self):
        """Test that null prices are removed"""
        df = pd.DataFrame({
            'symbol': ['AAPL', 'AAPL'],
            'date': [datetime.now(timezone.utc).date(), datetime.now(timezone.utc).date()],
            'close': [150.0, None],
            'asset_class': ['EQUITY', 'EQUITY'],
            'currency': ['USD', 'USD'],
            'exchange': ['NASDAQ', 'NASDAQ']
        })
        
        result = calculate_metrics(df)
        
        # Only one valid AAPL record
        assert len(result) == 1


class TestMetricsValidation:
    """Tests for metrics validation"""

    def test_validate_metrics_success(self, mock_price_data_df):
        """Test validation passes for valid metrics"""
        metrics_df = calculate_metrics(mock_price_data_df)
        
        # Should not raise exception
        assert validate_metrics(metrics_df) is True

    def test_validate_metrics_missing_columns(self):
        """Test validation fails for missing required columns"""
        invalid_df = pd.DataFrame({'symbol': ['AAPL']})
        
        with pytest.raises(ValueError, match="Missing required columns"):
            validate_metrics(invalid_df)

    def test_validate_metrics_null_critical_values(self):
        """Test validation fails for null critical values"""
        invalid_df = pd.DataFrame({
            'symbol': ['AAPL', None],
            'latest_price': [150.0, 151.0],
            'latest_date': [datetime.now().date(), datetime.now().date()],
            'price_52w_high': [155.0, 156.0],
            'price_52w_low': [120.0, 121.0],
            'asset_class': ['EQUITY', 'EQUITY'],
            'currency': ['USD', 'USD'],
            'version': [0, 0],
            'created_at': [datetime.now(timezone.utc), datetime.now(timezone.utc)],
            'last_updated': [datetime.now(timezone.utc), datetime.now(timezone.utc)],
            'updated_by': ['ETL_PROCESS', 'ETL_PROCESS']
        })
        
        with pytest.raises(ValueError, match="Null values found in critical columns"):
            validate_metrics(invalid_df)

    def test_validate_metrics_negative_price(self):
        """Test validation fails for negative prices"""
        invalid_df = pd.DataFrame({
            'symbol': ['AAPL'],
            'latest_price': [-150.0],
            'latest_date': [datetime.now().date()],
            'price_52w_high': [155.0],
            'price_52w_low': [120.0],
            'asset_class': ['EQUITY'],
            'currency': ['USD'],
            'version': [0],
            'created_at': [datetime.now(timezone.utc)],
            'last_updated': [datetime.now(timezone.utc)],
            'updated_by': ['ETL_PROCESS']
        })
        
        with pytest.raises(ValueError, match="latest_price must be greater than 0"):
            validate_metrics(invalid_df)

    def test_validate_metrics_high_less_than_low(self):
        """Test validation fails when 52w_high < 52w_low"""
        invalid_df = pd.DataFrame({
            'symbol': ['AAPL'],
            'latest_price': [150.0],
            'latest_date': [datetime.now().date()],
            'price_52w_high': [120.0],
            'price_52w_low': [155.0],
            'asset_class': ['EQUITY'],
            'currency': ['USD'],
            'version': [0],
            'created_at': [datetime.now(timezone.utc)],
            'last_updated': [datetime.now(timezone.utc)],
            'updated_by': ['ETL_PROCESS']
        })
        
        with pytest.raises(ValueError, match="52w_high cannot be less than 52w_low"):
            validate_metrics(invalid_df)


class TestLoadToDatabase:
    """Tests for database loading"""

    @patch('builtins.open', create=True)
    @patch('etl_instruments_metrics.DatabaseConnection')
    def test_load_to_database_success(self, mock_db_class, mock_open, mock_price_data_df):
        """Test successful database loading"""
        metrics_df = calculate_metrics(mock_price_data_df)
        
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_db = MagicMock()
        
        mock_db.get_connection.return_value.__enter__.return_value = mock_conn
        mock_conn.cursor.return_value.__enter__.return_value = mock_cursor
        
        # Mock file reading
        mock_open.return_value.__enter__.return_value.read.return_value = "SELECT 1;"
        
        records = load_to_database(mock_db, metrics_df)
        
        assert records == len(metrics_df)
        assert mock_cursor.execute.called


class TestETLPipeline:
    """Tests for complete ETL pipeline"""

    @patch('etl_instruments_metrics.load_to_database')
    @patch('etl_instruments_metrics.validate_metrics')
    @patch('etl_instruments_metrics.calculate_metrics')
    @patch('etl_instruments_metrics.extract_price_data')
    @patch('etl_instruments_metrics.DatabaseConnection')
    def test_run_etl_pipeline_success(self, mock_db_class, mock_extract, 
                                      mock_calculate, mock_validate, mock_load,
                                      mock_price_data_df):
        """Test successful ETL pipeline execution"""
        metrics_df = pd.DataFrame({
            'symbol': ['AAPL', 'GOOGL'],
            'latest_price': [150.0, 140.0],
            'version': [0, 0],
            'created_at': [datetime.now(timezone.utc), datetime.now(timezone.utc)],
            'updated_by': ['ETL_PROCESS', 'ETL_PROCESS']
        })
        
        mock_extract.return_value = mock_price_data_df
        mock_calculate.return_value = metrics_df
        mock_validate.return_value = True
        mock_load.return_value = 2
        
        result = run_etl_pipeline()
        
        assert result['status'] == 'success'
        assert result['records_loaded'] == 2
        assert result['unique_symbols'] == 2
        assert 'duration_seconds' in result

    @patch('etl_instruments_metrics.extract_price_data')
    @patch('etl_instruments_metrics.DatabaseConnection')
    def test_run_etl_pipeline_empty_price_data(self, mock_db_class, mock_extract):
        """Test ETL pipeline with empty price data"""
        mock_extract.return_value = pd.DataFrame()
        
        result = run_etl_pipeline()
        
        assert result['status'] == 'warning'
        assert result['message'] == 'No price data to process'

    @patch('etl_instruments_metrics.load_to_database')
    @patch('etl_instruments_metrics.validate_metrics')
    @patch('etl_instruments_metrics.calculate_metrics')
    @patch('etl_instruments_metrics.extract_price_data')
    @patch('etl_instruments_metrics.DatabaseConnection')
    def test_run_etl_pipeline_empty_metrics(self, mock_db_class, mock_extract,
                                            mock_calculate, mock_validate, mock_load,
                                            mock_price_data_df):
        """Test ETL pipeline with no calculated metrics"""
        mock_extract.return_value = mock_price_data_df
        mock_calculate.return_value = pd.DataFrame()
        
        result = run_etl_pipeline()
        
        assert result['status'] == 'warning'
        assert result['message'] == 'No metrics calculated'

    @patch('etl_instruments_metrics.load_to_database')
    @patch('etl_instruments_metrics.validate_metrics')
    @patch('etl_instruments_metrics.calculate_metrics')
    @patch('etl_instruments_metrics.extract_price_data')
    @patch('etl_instruments_metrics.DatabaseConnection')
    def test_run_etl_pipeline_validation_error(self, mock_db_class, mock_extract,
                                               mock_calculate, mock_validate, mock_load,
                                               mock_price_data_df):
        """Test ETL pipeline handles validation errors"""
        mock_extract.return_value = mock_price_data_df
        mock_calculate.return_value = pd.DataFrame({'symbol': ['AAPL']})
        mock_validate.side_effect = ValueError("Validation failed")
        
        result = run_etl_pipeline()
        
        assert result['status'] == 'error'
        assert 'error' in result

    @patch('etl_instruments_metrics.load_to_database')
    @patch('etl_instruments_metrics.validate_metrics')
    @patch('etl_instruments_metrics.calculate_metrics')
    @patch('etl_instruments_metrics.extract_price_data')
    @patch('etl_instruments_metrics.DatabaseConnection')
    def test_run_etl_pipeline_load_error(self, mock_db_class, mock_extract,
                                         mock_calculate, mock_validate, mock_load,
                                         mock_price_data_df):
        """Test ETL pipeline handles load errors"""
        mock_extract.return_value = mock_price_data_df
        mock_calculate.return_value = pd.DataFrame({'symbol': ['AAPL']})
        mock_validate.return_value = True
        mock_load.side_effect = Exception("Load failed")
        
        result = run_etl_pipeline()
        
        assert result['status'] == 'error'
        assert 'Load failed' in result['error']
