"""Unit tests for etl_price_metrics.py"""
import pytest
from unittest.mock import patch, MagicMock, call
import pandas as pd
from datetime import datetime, timezone
import sys
import os

# Add sql directory to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'etl'))

from etl_price_metrics import (
    DatabaseConnection,
    extract_clean_prices,
    calculate_daily_return,
    calculate_moving_average,
    calculate_volatility,
    calculate_volume_metrics,
    calculate_momentum_score,
    transform_clean_to_metrics,
    validate_metrics,
    load_to_database,
    run_etl_pipeline
)


@pytest.fixture
def mock_clean_prices_df():
    """Fixture: Mock clean prices data"""
    return pd.DataFrame({
        'ticker': ['AAPL', 'AAPL', 'AAPL', 'GOOGL', 'GOOGL'],
        'date': [datetime(2026, 1, 15), datetime(2026, 1, 16), datetime(2026, 1, 17),
                 datetime(2026, 1, 15), datetime(2026, 1, 16)],
        'open': [148.0, 150.0, 151.0, 138.0, 139.0],
        'high': [152.0, 153.0, 154.0, 142.0, 143.0],
        'low': [147.0, 149.0, 150.0, 137.0, 138.0],
        'close': [150.0, 151.0, 152.0, 140.0, 141.0],
        'volume': [1000000, 1100000, 1200000, 800000, 850000]
    })


@pytest.fixture
def mock_db_connection():
    """Fixture: Mock database connection"""
    with patch('etl_price_metrics.DatabaseConnection') as mock:
        yield mock


class TestDatabaseConnection:
    """Tests for DatabaseConnection class"""

    @patch('psycopg.connect')
    def test_connect_with_retry_success(self, mock_connect):
        """Test successful connection with retry"""
        mock_conn = MagicMock()
        mock_connect.return_value = mock_conn
        
        with patch.dict(os.environ, {
            'SPRING_DATASOURCE_URL': 'jdbc:postgresql://localhost:5432/testdb',
            'SPRING_DATASOURCE_USERNAME': 'testuser'
        }):
            db = DatabaseConnection()
            conn = db._connect_with_retry()
            assert conn == mock_conn
            mock_connect.assert_called_once()

    @patch('psycopg.connect')
    def test_connect_with_retry_failure(self, mock_connect):
        """Test connection retry after failures"""
        from psycopg.errors import Error as PsycopgError
        
        mock_connect.side_effect = PsycopgError("Connection failed")
        
        with patch.dict(os.environ, {
            'SPRING_DATASOURCE_URL': 'jdbc:postgresql://localhost:5432/testdb',
            'SPRING_DATASOURCE_USERNAME': 'testuser'
        }):
            db = DatabaseConnection(max_retries=2)
            
            with pytest.raises(PsycopgError):
                db._connect_with_retry()
            
            # Should retry 2 times
            assert mock_connect.call_count == 2


class TestDataExtraction:
    """Tests for data extraction functions"""

    @patch('etl_price_metrics.DatabaseConnection')
    def test_extract_clean_prices_success(self, mock_db_class, mock_clean_prices_df):
        """Test successful price extraction"""
        mock_cursor = MagicMock()
        mock_cursor.description = [('ticker',), ('date',), ('open',), ('high',), ('low',), ('close',), ('volume',)]
        mock_cursor.fetchall.return_value = mock_clean_prices_df.values.tolist()
        
        mock_db = MagicMock()
        mock_db.get_cursor.return_value.__enter__.return_value = mock_cursor
        
        result = extract_clean_prices(mock_db, lookback_days=365)
        
        assert isinstance(result, pd.DataFrame)
        assert len(result) == 5
        mock_cursor.execute.assert_called_once()

    @patch('etl_price_metrics.DatabaseConnection')
    def test_extract_clean_prices_empty(self, mock_db_class):
        """Test extraction returns empty DataFrame when no data"""
        mock_cursor = MagicMock()
        mock_cursor.description = [('ticker',)]
        mock_cursor.fetchall.return_value = []
        
        mock_db = MagicMock()
        mock_db.get_cursor.return_value.__enter__.return_value = mock_cursor
        
        result = extract_clean_prices(mock_db, lookback_days=365)
        
        assert isinstance(result, pd.DataFrame)
        assert len(result) == 0


class TestMetricCalculations:
    """Tests for metric calculation functions"""

    def test_calculate_daily_return(self, mock_clean_prices_df):
        """Test daily return calculation"""
        aapl_data = mock_clean_prices_df[mock_clean_prices_df['ticker'] == 'AAPL'].copy()
        
        result = calculate_daily_return(aapl_data)
        
        assert len(result) == len(aapl_data)
        # First value should be NaN
        assert pd.isna(result.iloc[0])
        # Second value should be (151-150)/150
        assert abs(result.iloc[1] - 0.006667) < 0.001

    def test_calculate_moving_average(self, mock_clean_prices_df):
        """Test moving average calculation"""
        aapl_data = mock_clean_prices_df[mock_clean_prices_df['ticker'] == 'AAPL'].copy()
        
        result = calculate_moving_average(aapl_data, window=2)
        
        assert len(result) == len(aapl_data)
        assert result.iloc[0] == 150.0  # First close price

    def test_calculate_volatility(self, mock_clean_prices_df):
        """Test volatility calculation"""
        aapl_data = mock_clean_prices_df[mock_clean_prices_df['ticker'] == 'AAPL'].copy()
        
        result = calculate_volatility(aapl_data, window=2)
        
        assert len(result) == len(aapl_data)
        # Should have NaN values at start
        assert pd.isna(result.iloc[0])

    def test_calculate_volume_metrics(self, mock_clean_prices_df):
        """Test volume metrics calculation"""
        aapl_data = mock_clean_prices_df[mock_clean_prices_df['ticker'] == 'AAPL'].copy()
        
        avg_vol, vol_ratio = calculate_volume_metrics(aapl_data, window=2)
        
        assert len(avg_vol) == len(aapl_data)
        assert len(vol_ratio) == len(aapl_data)
        assert (vol_ratio > 0).all()

    def test_calculate_momentum_score(self):
        """Test momentum score calculation"""
        returns = pd.Series([0.01, 0.02, -0.01, 0.015, 0.005])
        volatility = pd.Series([0.02, 0.03, 0.02, 0.025, 0.02])
        price_trend = pd.Series([100, 101, 102.02, 101, 102.52])
        
        result = calculate_momentum_score(returns, volatility, price_trend)
        
        assert len(result) == len(returns)
        assert (-100 <= result).all() and (result <= 100).all()


class TestDataTransformation:
    """Tests for data transformation"""

    def test_transform_clean_to_metrics_success(self, mock_clean_prices_df):
        """Test successful transformation to metrics"""
        result = transform_clean_to_metrics(mock_clean_prices_df)
        
        assert isinstance(result, pd.DataFrame)
        assert 'ticker' in result.columns
        assert 'trade_date' in result.columns
        assert 'close_price' in result.columns
        assert 'daily_return' in result.columns
        assert 'moving_avg_20' in result.columns
        assert 'moving_avg_50' in result.columns
        assert 'volatility_30d' in result.columns
        assert 'momentum_score' in result.columns
        assert 'version' in result.columns
        assert 'created_at' in result.columns
        assert 'updated_by' in result.columns

    def test_transform_clean_to_metrics_adds_audit_columns(self, mock_clean_prices_df):
        """Test that audit columns are added during transformation"""
        result = transform_clean_to_metrics(mock_clean_prices_df)
        
        # Check audit columns exist
        assert (result['version'] == 0).all()
        assert result['created_at'].notna().all()
        assert result['updated_by'].eq('ETL_PROCESS').all()


class TestMetricsValidation:
    """Tests for metrics validation"""

    def test_validate_metrics_success(self, mock_clean_prices_df):
        """Test validation passes for valid metrics"""
        metrics_df = transform_clean_to_metrics(mock_clean_prices_df)
        
        # Should not raise exception
        assert validate_metrics(metrics_df) is True

    def test_validate_metrics_missing_columns(self):
        """Test validation fails for missing columns"""
        invalid_df = pd.DataFrame({'ticker': ['AAPL']})
        
        with pytest.raises(ValueError, match="Missing required columns"):
            validate_metrics(invalid_df)

    def test_validate_metrics_null_in_critical(self):
        """Test validation fails for null values in critical columns"""
        invalid_df = pd.DataFrame({
            'ticker': ['AAPL', None],
            'trade_date': [datetime.now(), datetime.now()],
            'close_price': [150.0, 151.0],
            'daily_return': [0.01, 0.02],
            'moving_avg_20': [150.0, 151.0],
            'moving_avg_50': [149.0, 150.0],
            'avg_volume_30d': [1000000, 1100000],
            'volatility_30d': [0.02, 0.03],
            'volume_spike_ratio': [1.0, 1.1],
            'momentum_score': [50.0, 60.0],
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
            'ticker': ['AAPL'],
            'trade_date': [datetime.now()],
            'close_price': [-150.0],
            'daily_return': [0.01],
            'moving_avg_20': [150.0],
            'moving_avg_50': [149.0],
            'avg_volume_30d': [1000000],
            'volatility_30d': [0.02],
            'volume_spike_ratio': [1.0],
            'momentum_score': [50.0],
            'version': [0],
            'created_at': [datetime.now(timezone.utc)],
            'last_updated': [datetime.now(timezone.utc)],
            'updated_by': ['ETL_PROCESS']
        })
        
        with pytest.raises(ValueError, match="close_price must be greater than 0"):
            validate_metrics(invalid_df)


class TestETLPipeline:
    """Tests for complete ETL pipeline"""

    @patch('etl_price_metrics.load_to_database')
    @patch('etl_price_metrics.validate_metrics')
    @patch('etl_price_metrics.transform_clean_to_metrics')
    @patch('etl_price_metrics.extract_clean_prices')
    @patch('etl_price_metrics.DatabaseConnection')
    def test_run_etl_pipeline_success(self, mock_db_class, mock_extract, 
                                      mock_transform, mock_validate, mock_load,
                                      mock_clean_prices_df):
        """Test successful ETL pipeline execution"""
        mock_extract.return_value = mock_clean_prices_df
        mock_transform.return_value = pd.DataFrame({'ticker': ['AAPL']})
        mock_validate.return_value = True
        mock_load.return_value = 2
        
        result = run_etl_pipeline(lookback_days=365)
        
        assert result['status'] == 'success'
        assert result['records_loaded'] == 2
        assert 'duration_seconds' in result

    @patch('etl_price_metrics.extract_clean_prices')
    @patch('etl_price_metrics.DatabaseConnection')
    def test_run_etl_pipeline_empty_data(self, mock_db_class, mock_extract):
        """Test ETL pipeline with empty data"""
        mock_extract.return_value = pd.DataFrame()
        
        result = run_etl_pipeline()
        
        assert result['status'] == 'warning'
        assert result['message'] == 'No data to process'

    @patch('etl_price_metrics.load_to_database')
    @patch('etl_price_metrics.validate_metrics')
    @patch('etl_price_metrics.transform_clean_to_metrics')
    @patch('etl_price_metrics.extract_clean_prices')
    @patch('etl_price_metrics.DatabaseConnection')
    def test_run_etl_pipeline_validation_error(self, mock_db_class, mock_extract,
                                               mock_transform, mock_validate, mock_load,
                                               mock_clean_prices_df):
        """Test ETL pipeline handles validation errors"""
        mock_extract.return_value = mock_clean_prices_df
        mock_transform.return_value = pd.DataFrame({'ticker': ['AAPL']})
        mock_validate.side_effect = ValueError("Validation failed")
        
        result = run_etl_pipeline()
        
        assert result['status'] == 'error'
        assert 'error' in result
