from types import SimpleNamespace
from unittest.mock import MagicMock

import pandas as pd


class FakePsycopgError(Exception):
    pass


def make_fake_psycopg(connect_result=None, connect_side_effect=None):
    fake = SimpleNamespace()
    fake.Error = FakePsycopgError
    if connect_side_effect is not None:
        fake.connect = MagicMock(side_effect=connect_side_effect)
    else:
        fake.connect = MagicMock(return_value=connect_result)
    return fake


def make_ohlcv_df(rows):
    """rows: list of (date_str, open, high, low, close, volume)."""
    index = pd.to_datetime([r[0] for r in rows])
    data = {
        "Open": [r[1] for r in rows],
        "High": [r[2] for r in rows],
        "Low": [r[3] for r in rows],
        "Close": [r[4] for r in rows],
        "Volume": [r[5] for r in rows],
    }
    return pd.DataFrame(data, index=index)


# ---------------------------------------------------------------------------
# get_db_connection
# ---------------------------------------------------------------------------

def test_get_db_connection_success(prices_module, monkeypatch):
    fake_conn = MagicMock()
    monkeypatch.setattr(prices_module, "psycopg", make_fake_psycopg(connect_result=fake_conn))

    assert prices_module.get_db_connection() is fake_conn


def test_get_db_connection_failure_returns_none(prices_module, monkeypatch):
    monkeypatch.setattr(prices_module, "psycopg", make_fake_psycopg(connect_side_effect=FakePsycopgError("x")))

    assert prices_module.get_db_connection() is None


# ---------------------------------------------------------------------------
# load_instruments
# ---------------------------------------------------------------------------

def test_load_instruments_returns_symbols(prices_module, monkeypatch):
    fake_cursor = MagicMock()
    fake_cursor.fetchall.return_value = [("AAPL",), ("MSFT",)]
    fake_conn = MagicMock()
    fake_conn.cursor.return_value = fake_cursor
    monkeypatch.setattr(prices_module, "get_db_connection", lambda: fake_conn)

    result = prices_module.load_instruments()

    assert result == ["AAPL", "MSFT"]
    fake_conn.close.assert_called_once()


def test_load_instruments_no_connection_returns_empty(prices_module, monkeypatch):
    monkeypatch.setattr(prices_module, "get_db_connection", lambda: None)

    assert prices_module.load_instruments() == []


def test_load_instruments_db_error_returns_empty_and_closes(prices_module, monkeypatch):
    fake_conn = MagicMock()
    fake_conn.cursor.side_effect = FakePsycopgError("fail")
    monkeypatch.setattr(prices_module, "get_db_connection", lambda: fake_conn)
    monkeypatch.setattr(prices_module, "psycopg", make_fake_psycopg())

    result = prices_module.load_instruments()

    assert result == []
    fake_conn.close.assert_called_once()


# ---------------------------------------------------------------------------
# fetch_yfinance_data
# ---------------------------------------------------------------------------

def test_fetch_yfinance_data_success(prices_module, monkeypatch):
    df = make_ohlcv_df([("2024-01-01", 1, 2, 0.5, 1.5, 100)])
    monkeypatch.setattr(prices_module, "yf", SimpleNamespace(download=lambda *a, **k: df))

    result = prices_module.fetch_yfinance_data(["AAPL"], days=10)

    assert list(result.keys()) == ["AAPL"]
    pd.testing.assert_frame_equal(result["AAPL"], df)


def test_fetch_yfinance_data_skips_symbols_with_no_data(prices_module, monkeypatch):
    monkeypatch.setattr(prices_module, "yf", SimpleNamespace(download=lambda *a, **k: pd.DataFrame()))

    result = prices_module.fetch_yfinance_data(["AAPL"], days=10)

    assert result == {}


def test_fetch_yfinance_data_handles_exceptions_per_symbol(prices_module, monkeypatch):
    df = make_ohlcv_df([("2024-01-01", 1, 2, 0.5, 1.5, 100)])

    def fake_download(symbol, start, end, progress):
        if symbol == "AAPL":
            return df
        raise RuntimeError("network error")

    monkeypatch.setattr(prices_module, "yf", SimpleNamespace(download=fake_download))

    result = prices_module.fetch_yfinance_data(["AAPL", "BROKEN"], days=10)

    assert list(result.keys()) == ["AAPL"]


def test_fetch_yfinance_data_passes_expected_date_range(prices_module, monkeypatch):
    captured = {}

    def fake_download(symbol, start, end, progress):
        captured["start"] = start
        captured["end"] = end
        captured["progress"] = progress
        return make_ohlcv_df([("2024-01-01", 1, 2, 0.5, 1.5, 10)])

    monkeypatch.setattr(prices_module, "yf", SimpleNamespace(download=fake_download))

    prices_module.fetch_yfinance_data(["AAPL"], days=30)

    assert captured["progress"] is False
    assert (captured["end"] - captured["start"]).days == 30


# ---------------------------------------------------------------------------
# generate_prices
# ---------------------------------------------------------------------------

def test_generate_prices_converts_row_fields(prices_module):
    open_val, high_val, low_val, close_val, volume_val = 1.234567, 2.345678, 0.555512, 1.987654, 1000
    df = make_ohlcv_df([("2024-01-01", open_val, high_val, low_val, close_val, volume_val)])

    prices = prices_module.generate_prices({"AAPL": df})

    assert len(prices) == 1
    row = prices[0]
    assert row["symbol"] == "AAPL"
    assert row["date"] == "2024-01-01"
    assert row["open"] == round(open_val, 4)
    assert row["high"] == round(high_val, 4)
    assert row["low"] == round(low_val, 4)
    assert row["close"] == round(close_val, 4)
    assert row["volume"] == volume_val
    assert isinstance(row["volume"], int)
    assert row["version"] == 0
    assert row["updated_by"] == "SYSTEM"
    assert "created_at" in row
    assert "last_updated" in row


def test_generate_prices_handles_multiple_symbols_and_days(prices_module):
    df1 = make_ohlcv_df([
        ("2024-01-01", 1, 2, 0.5, 1.5, 100),
        ("2024-01-02", 1.5, 2.5, 1.0, 2.0, 200),
    ])
    df2 = make_ohlcv_df([("2024-01-01", 10, 11, 9, 10.5, 50)])

    prices = prices_module.generate_prices({"AAPL": df1, "MSFT": df2})

    assert len(prices) == 3
    assert {p["symbol"] for p in prices} == {"AAPL", "MSFT"}


def test_generate_prices_empty_input_returns_empty_list(prices_module):
    assert prices_module.generate_prices({}) == []


# ---------------------------------------------------------------------------
# insert_prices_to_db
# ---------------------------------------------------------------------------

def test_insert_prices_to_db_no_prices_returns_false_without_connecting(prices_module, monkeypatch):
    connect_mock = MagicMock()
    monkeypatch.setattr(prices_module, "get_db_connection", connect_mock)

    result = prices_module.insert_prices_to_db([])

    assert result is False
    connect_mock.assert_not_called()


def test_insert_prices_to_db_no_connection_returns_false(prices_module, monkeypatch):
    monkeypatch.setattr(prices_module, "get_db_connection", lambda: None)
    prices = [{
        "symbol": "AAPL", "date": "2024-01-01", "open": 1, "high": 2, "low": 0.5,
        "close": 1.5, "volume": 100, "version": 0, "created_at": "now",
        "last_updated": "now", "updated_by": "SYSTEM",
    }]

    assert prices_module.insert_prices_to_db(prices) is False


def test_insert_prices_to_db_success(prices_module, monkeypatch):
    fake_cursor = MagicMock()
    fake_conn = MagicMock()
    fake_conn.cursor.return_value = fake_cursor
    monkeypatch.setattr(prices_module, "get_db_connection", lambda: fake_conn)

    prices = [{
        "symbol": "AAPL", "date": "2024-01-01", "open": 1, "high": 2, "low": 0.5,
        "close": 1.5, "volume": 100, "version": 0, "created_at": "now",
        "last_updated": "now", "updated_by": "SYSTEM",
    }]

    result = prices_module.insert_prices_to_db(prices)

    assert result is True
    fake_cursor.execute.assert_called_once()
    fake_conn.commit.assert_called_once()
    fake_conn.close.assert_called_once()


def test_insert_prices_to_db_error_rolls_back(prices_module, monkeypatch):
    fake_cursor = MagicMock()
    fake_cursor.execute.side_effect = FakePsycopgError("bad insert")
    fake_conn = MagicMock()
    fake_conn.cursor.return_value = fake_cursor
    monkeypatch.setattr(prices_module, "get_db_connection", lambda: fake_conn)
    monkeypatch.setattr(prices_module, "psycopg", make_fake_psycopg())

    prices = [{
        "symbol": "AAPL", "date": "2024-01-01", "open": 1, "high": 2, "low": 0.5,
        "close": 1.5, "volume": 100, "version": 0, "created_at": "now",
        "last_updated": "now", "updated_by": "SYSTEM",
    }]

    result = prices_module.insert_prices_to_db(prices)

    assert result is False
    fake_conn.rollback.assert_called_once()
    fake_conn.close.assert_called_once()


# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------

def test_main_exits_when_no_instruments(prices_module, monkeypatch):
    monkeypatch.setattr(prices_module, "load_instruments", lambda: [])
    fetch_mock = MagicMock()
    monkeypatch.setattr(prices_module, "fetch_yfinance_data", fetch_mock)

    prices_module.main()

    fetch_mock.assert_not_called()


def test_main_exits_when_no_yf_data(prices_module, monkeypatch):
    monkeypatch.setattr(prices_module, "load_instruments", lambda: ["AAPL"])
    monkeypatch.setattr(prices_module, "fetch_yfinance_data", lambda symbols: {})
    insert_mock = MagicMock()
    monkeypatch.setattr(prices_module, "insert_prices_to_db", insert_mock)

    prices_module.main()

    insert_mock.assert_not_called()


def test_main_happy_path_inserts_generated_prices(prices_module, monkeypatch):
    df = make_ohlcv_df([("2024-01-01", 1, 2, 0.5, 1.5, 100)])
    monkeypatch.setattr(prices_module, "load_instruments", lambda: ["AAPL"])
    monkeypatch.setattr(prices_module, "fetch_yfinance_data", lambda symbols: {"AAPL": df})
    insert_mock = MagicMock(return_value=True)
    monkeypatch.setattr(prices_module, "insert_prices_to_db", insert_mock)

    prices_module.main()

    inserted_prices = insert_mock.call_args[0][0]
    assert len(inserted_prices) == 1
    assert inserted_prices[0]["symbol"] == "AAPL"
