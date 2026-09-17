import importlib.util
from pathlib import Path

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
