"""Every python script reads its DB connection from the SPRING_DATASOURCE_* variables."""
import importlib.util
from pathlib import Path
from unittest.mock import patch

import pytest

DB_DIR = Path(__file__).resolve().parent.parent
ETL_DIR = DB_DIR / "etl"

SCRIPTS = [
    ETL_DIR / "01-generate_raw_instruments.py",
    ETL_DIR / "02-generate_raw_prices.py",
    ETL_DIR / "03-etl_prices.py",
    ETL_DIR / "04-etl_stocks.py",
    ETL_DIR / "05-etl_etfs.py",
    ETL_DIR / "06-etl_bonds.py",
    ETL_DIR / "etl_instruments_metrics.py",
    ETL_DIR / "etl_price_metrics.py",
    DB_DIR / "dashboard" / "data" / "loaders.py",
]

ENV = {
    "SPRING_DATASOURCE_URL": "jdbc:postgresql://instruments-db:5432/instruments_db",
    "SPRING_DATASOURCE_USERNAME": "svc_user",
    "SPRING_DATASOURCE_PASSWORD": "svc_pass",
}


def _load(path, env):
    with patch.dict("os.environ", env, clear=True):
        spec = importlib.util.spec_from_file_location("_cfg_" + path.stem.replace("-", "_"), path)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
    return module


@pytest.mark.parametrize("path", SCRIPTS, ids=lambda p: p.name)
def test_connection_settings_come_from_spring_datasource(path):
    module = _load(path, ENV)
    assert module.DB_HOST == "instruments-db"
    assert module.DB_PORT == "5432"
    assert module.DB_NAME == "instruments_db"
    assert module.DB_USER == "svc_user"
    assert module.DB_PASSWORD == "svc_pass"


@pytest.mark.parametrize("path", SCRIPTS, ids=lambda p: p.name)
def test_non_default_port_is_parsed(path):
    env = {**ENV, "SPRING_DATASOURCE_URL": "jdbc:postgresql://localhost:5434/instruments_db"}
    module = _load(path, env)
    assert (module.DB_HOST, module.DB_PORT) == ("localhost", "5434")


@pytest.mark.parametrize("path", SCRIPTS[-3:-1], ids=lambda p: p.name)
def test_metrics_etls_exit_without_datasource_settings(path):
    with pytest.raises(SystemExit):
        _load(path, {})
