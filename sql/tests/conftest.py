import importlib.util
from pathlib import Path
from datetime import date
from unittest.mock import MagicMock, patch

import pytest

SQL_DIR = Path(__file__).resolve().parent.parent

#docker exec underfrog-dashboard python -m pytest /sql/tests -v

def _load_module(filename):
    """Load a sql/ script as a module.

    Filenames like '01-generate_raw_prices.py' aren't valid module names
    (leading digit, hyphen), so they can't be `import`-ed normally.
    """
    module_name = "_sql_" + filename[:-3].replace("-", "_")
    module_path = SQL_DIR / filename
    spec = importlib.util.spec_from_file_location(module_name, module_path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


@pytest.fixture
def instruments_module():
    """Fresh copy of 01-generate_raw_instruments.py for each test."""
    return _load_module("01-generate_raw_instruments.py")


@pytest.fixture
def prices_module():
    """Fresh copy of 02-generate_raw_prices.py for each test."""
    return _load_module("02-generate_raw_prices.py")


@pytest.fixture
def etl_prices_module():
    """Fresh copy of 03-etl_prices.py for each test."""
    return _load_module("03-etl_prices.py")


@pytest.fixture
def etl_stocks_module():
    """Fresh copy of 04-etl_stocks.py for each test."""
    return _load_module("04-etl_stocks.py")


@pytest.fixture
def etl_etfs_module():
    """Fresh copy of 05-etl_etfs.py for each test."""
    return _load_module("05-etl_etfs.py")


@pytest.fixture
def etl_bonds_module():
    """Fresh copy of 06-etl_bonds.py for each test."""
    return _load_module("06-etl_bonds.py")


# ============================================================================
# Shared fixtures for ETL testing (03-06)
# ============================================================================

@pytest.fixture
def mock_db():
    """Fixture: Mock database object with connection and cursor"""
    db = MagicMock()
    cursor = MagicMock()
    cursor.fetchall.return_value = []
    cursor.commit = MagicMock()
    cursor.close = MagicMock()
    
    db.cursor.return_value = cursor
    db.commit = MagicMock()
    db.close = MagicMock()
    
    return db


@pytest.fixture
def sample_price_row():
    """Fixture: Sample valid price row tuple"""
    return ('AAPL', date(2024, 1, 1), 150.0, 155.0, 145.0, 152.0, 1000000)


@pytest.fixture
def sample_stock_dict():
    """Fixture: Sample valid stock record"""
    return {
        'symbol': 'AAPL',
        'sector': 'Technology',
        'industry': 'Consumer Electronics',
        'country': 'USA',
        'market_cap': 3000000000000,
        'shares_outstanding': 15600000000,
        'full_time_employees': 164000,
        'trailing_pe': 28.5,
        'forward_pe': 26.0,
        'trailing_eps': 6.05,
        'dividend_rate': 0.96,
        'payout_ratio': 15.8,
        'price_to_book': 46.5,
        'return_on_equity': 165.5,
        'total_revenue': 394328000000,
        'website': 'https://www.apple.com'
    }


@pytest.fixture
def sample_etf_dict():
    """Fixture: Sample valid ETF record"""
    return {
        'symbol': 'SPY',
        'category': 'Large Cap Equity',
        'fund_family': 'SPDR',
        'legal_type': 'Exchange Traded Fund',
        'net_expense_ratio': 0.03,
        'nav_price': 450.25,
        'total_assets': 500000000000,
        'net_assets': 500000000000,
        'ytd_return': 15.5,
        'three_year_avg_return': 10.2,
        'five_year_avg_return': 12.8,
        'beta_3_year': 1.0,
        'distribution_yield': 1.5
    }


@pytest.fixture
def sample_bond_dict():
    """Fixture: Sample valid bond record"""
    return {
        'symbol': 'BND',
        'category': 'Aggregate Bond',
        'fund_family': 'Vanguard',
        'legal_type': 'Exchange Traded Fund',
        'net_expense_ratio': 0.03,
        'nav_price': 75.50,
        'total_assets': 100000000000,
        'net_assets': 100000000000,
        'ytd_return': 2.5,
        'three_year_avg_return': 1.8,
        'five_year_avg_return': 2.2,
        'beta_3_year': 0.1,
        'distribution_yield': 3.5
    }
