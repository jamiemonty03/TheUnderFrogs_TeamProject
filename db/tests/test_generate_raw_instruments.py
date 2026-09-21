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


# ---------------------------------------------------------------------------
# classify_instrument
# ---------------------------------------------------------------------------

def test_classify_instrument_equity(instruments_module):
    assert instruments_module.classify_instrument({"quoteType": "EQUITY"}) == "Equity"


def test_classify_instrument_bond_etf_by_category(instruments_module):
    info = {"quoteType": "ETF", "category": "Long Government Bond"}
    assert instruments_module.classify_instrument(info) == "Bond"


def test_classify_instrument_plain_etf(instruments_module):
    info = {"quoteType": "ETF", "category": "Large Blend"}
    assert instruments_module.classify_instrument(info) == "ETF"


def test_classify_instrument_etf_missing_category_defaults_to_etf(instruments_module):
    assert instruments_module.classify_instrument({"quoteType": "ETF"}) == "ETF"


def test_classify_instrument_category_match_is_case_insensitive(instruments_module):
    info = {"quoteType": "ETF", "category": "INFLATION-PROTECTED BOND"}
    assert instruments_module.classify_instrument(info) == "Bond"


def test_classify_instrument_unknown_quote_type(instruments_module):
    assert instruments_module.classify_instrument({"quoteType": "CRYPTOCURRENCY"}) == "unknown"


def test_classify_instrument_missing_quote_type(instruments_module):
    assert instruments_module.classify_instrument({}) == "unknown"


# ---------------------------------------------------------------------------
# build_stock_row / build_fund_row
# ---------------------------------------------------------------------------

def test_build_stock_row_extracts_fields(instruments_module):
    info = {
        "sector": "Technology", "industry": "Software", "country": "US",
        "marketCap": 1000, "sharesOutstanding": 500, "fullTimeEmployees": 10,
        "beta": 1.2, "trailingPE": 20.5, "forwardPE": 18.0, "trailingEps": 5.0,
        "dividendRate": 0.5, "payoutRatio": 0.2, "priceToBook": 3.0,
        "returnOnEquity": 0.15, "totalRevenue": 100000, "website": "https://example.com",
    }
    row = instruments_module.build_stock_row("AAPL", info)

    assert row["symbol"] == "AAPL"
    assert row["sector"] == "Technology"
    assert row["trailing_pe"] == 20.5
    assert row["website"] == "https://example.com"


def test_build_stock_row_missing_fields_are_none(instruments_module):
    row = instruments_module.build_stock_row("AAPL", {})

    assert row["symbol"] == "AAPL"
    assert all(value is None for key, value in row.items() if key != "symbol")


def test_build_fund_row_extracts_fields(instruments_module):
    info = {
        "category": "Large Blend", "fundFamily": "Vanguard", "legalType": "Open Ended Fund",
        "netExpenseRatio": 0.03, "navPrice": 100.0, "totalAssets": 5_000_000,
        "netAssets": 4_900_000, "ytdReturn": 0.1, "threeYearAverageReturn": 0.08,
        "fiveYearAverageReturn": 0.09, "beta3Year": 1.0, "yield": 0.02,
    }
    row = instruments_module.build_fund_row("SPY", info)

    assert row["symbol"] == "SPY"
    assert row["fund_family"] == "Vanguard"
    assert row["distribution_yield"] == 0.02


def test_build_fund_row_missing_fields_are_none(instruments_module):
    row = instruments_module.build_fund_row("SPY", {})

    assert row["symbol"] == "SPY"
    assert all(value is None for key, value in row.items() if key != "symbol")


# ---------------------------------------------------------------------------
# clean_dataframe
# ---------------------------------------------------------------------------

def test_clean_dataframe_converts_nan_to_none(instruments_module):
    df = pd.DataFrame({"a": [1, None], "b": [None, "x"]})

    cleaned = instruments_module.clean_dataframe(df)

    assert cleaned.iloc[1]["a"] is None
    assert cleaned.iloc[0]["b"] is None
    assert cleaned.iloc[0]["a"] == 1


# ---------------------------------------------------------------------------
# build_dataframes
# ---------------------------------------------------------------------------

def test_build_dataframes_classifies_and_splits(instruments_module):
    yf_data = {
        "AAPL": {"quoteType": "EQUITY", "shortName": "Apple", "currency": "USD",
                  "fullExchangeName": "NASDAQ", "sector": "Tech"},
        "SPY": {"quoteType": "ETF", "category": "Large Blend", "shortName": "SPDR S&P 500",
                 "currency": "USD", "fullExchangeName": "NYSE"},
        "TLT": {"quoteType": "ETF", "category": "Long Government Bond", "shortName": "Treasury",
                 "currency": "USD", "fullExchangeName": "NASDAQ"},
        "XYZ": {"quoteType": "CRYPTOCURRENCY"},
    }

    instruments_df, stocks_df, etfs_df, bonds_df = instruments_module.build_dataframes(yf_data)

    assert set(instruments_df["symbol"]) == {"AAPL", "SPY", "TLT"}
    assert len(stocks_df) == 1 and stocks_df.iloc[0]["symbol"] == "AAPL"
    assert len(etfs_df) == 1 and etfs_df.iloc[0]["symbol"] == "SPY"
    assert len(bonds_df) == 1 and bonds_df.iloc[0]["symbol"] == "TLT"

    aapl_row = instruments_df.loc[instruments_df["symbol"] == "AAPL"].iloc[0]
    assert aapl_row["asset_class"] == "Equity"
    assert aapl_row["name"] == "Apple"


def test_build_dataframes_falls_back_to_symbol_when_name_missing(instruments_module):
    yf_data = {"AAPL": {"quoteType": "EQUITY"}}

    instruments_df, *_ = instruments_module.build_dataframes(yf_data)

    assert instruments_df.iloc[0]["name"] == "AAPL"


# ---------------------------------------------------------------------------
# get_db_connection
# ---------------------------------------------------------------------------

def test_get_db_connection_success(instruments_module, monkeypatch):
    fake_conn = MagicMock()
    monkeypatch.setattr(instruments_module, "psycopg", make_fake_psycopg(connect_result=fake_conn))

    assert instruments_module.get_db_connection() is fake_conn


def test_get_db_connection_failure_returns_none(instruments_module, monkeypatch):
    fake = make_fake_psycopg(connect_side_effect=FakePsycopgError("boom"))
    monkeypatch.setattr(instruments_module, "psycopg", fake)

    assert instruments_module.get_db_connection() is None


# ---------------------------------------------------------------------------
# load_instruments
# ---------------------------------------------------------------------------

def test_load_instruments_returns_symbols(instruments_module, monkeypatch):
    fake_cursor = MagicMock()
    fake_cursor.fetchall.return_value = [("AAPL",), ("MSFT",)]
    fake_conn = MagicMock()
    fake_conn.cursor.return_value = fake_cursor
    monkeypatch.setattr(instruments_module, "get_db_connection", lambda: fake_conn)

    result = instruments_module.load_instruments()

    assert result == ["AAPL", "MSFT"]
    fake_conn.close.assert_called_once()


def test_load_instruments_no_connection_returns_empty(instruments_module, monkeypatch):
    monkeypatch.setattr(instruments_module, "get_db_connection", lambda: None)

    assert instruments_module.load_instruments() == []


def test_load_instruments_db_error_returns_empty_and_closes(instruments_module, monkeypatch):
    fake_conn = MagicMock()
    fake_conn.cursor.side_effect = FakePsycopgError("fail")
    monkeypatch.setattr(instruments_module, "get_db_connection", lambda: fake_conn)
    monkeypatch.setattr(instruments_module, "psycopg", make_fake_psycopg())

    result = instruments_module.load_instruments()

    assert result == []
    fake_conn.close.assert_called_once()


# ---------------------------------------------------------------------------
# fetch_yfinance_data
# ---------------------------------------------------------------------------

def test_fetch_yfinance_data_success_and_skips_bad_symbols(instruments_module, monkeypatch):
    info_map = {
        "AAPL": {"quoteType": "EQUITY"},
        "BAD": {},
        "NOPE": {"quoteType": None},
    }

    def fake_ticker(symbol):
        return SimpleNamespace(info=info_map[symbol])

    monkeypatch.setattr(instruments_module, "yf", SimpleNamespace(Ticker=fake_ticker))

    result = instruments_module.fetch_yfinance_data(["AAPL", "BAD", "NOPE"])

    assert result == {"AAPL": {"quoteType": "EQUITY"}}


def test_fetch_yfinance_data_handles_exceptions_per_symbol(instruments_module, monkeypatch):
    def fake_ticker(symbol):
        if symbol == "AAPL":
            return SimpleNamespace(info={"quoteType": "EQUITY"})
        raise RuntimeError("network error")

    monkeypatch.setattr(instruments_module, "yf", SimpleNamespace(Ticker=fake_ticker))

    result = instruments_module.fetch_yfinance_data(["AAPL", "BROKEN"])

    assert result == {"AAPL": {"quoteType": "EQUITY"}}


# ---------------------------------------------------------------------------
# insert_to_db
# ---------------------------------------------------------------------------

def test_insert_to_db_success(instruments_module, monkeypatch):
    fake_cursor = MagicMock()
    fake_conn = MagicMock()
    fake_conn.cursor.return_value = fake_cursor
    monkeypatch.setattr(instruments_module, "get_db_connection", lambda: fake_conn)

    instruments_df = pd.DataFrame([{
        "symbol": "AAPL", "name": "Apple", "asset_class": "Equity",
        "currency": "USD", "exchange": "NASDAQ",
    }])
    stocks_df = pd.DataFrame([{"symbol": "AAPL", "sector": "Tech"}])
    empty = pd.DataFrame(columns=["symbol"])

    result = instruments_module.insert_to_db(instruments_df, stocks_df, empty, empty)

    assert result is True
    fake_conn.commit.assert_called_once()
    fake_conn.close.assert_called_once()
    # 1 upsert into instruments + 3 deletes (raw_stocks/etfs/bonds) + 1 insert into raw_stocks
    assert fake_cursor.execute.call_count == 5


def test_insert_to_db_no_connection_returns_false(instruments_module, monkeypatch):
    monkeypatch.setattr(instruments_module, "get_db_connection", lambda: None)

    result = instruments_module.insert_to_db(
        pd.DataFrame(), pd.DataFrame(), pd.DataFrame(), pd.DataFrame()
    )

    assert result is False


def test_insert_to_db_error_rolls_back(instruments_module, monkeypatch):
    fake_cursor = MagicMock()
    fake_cursor.execute.side_effect = FakePsycopgError("bad insert")
    fake_conn = MagicMock()
    fake_conn.cursor.return_value = fake_cursor
    monkeypatch.setattr(instruments_module, "get_db_connection", lambda: fake_conn)
    monkeypatch.setattr(instruments_module, "psycopg", make_fake_psycopg())

    instruments_df = pd.DataFrame([{
        "symbol": "AAPL", "name": "Apple", "asset_class": "Equity",
        "currency": "USD", "exchange": "NASDAQ",
    }])
    empty = pd.DataFrame(columns=["symbol"])

    result = instruments_module.insert_to_db(instruments_df, empty, empty, empty)

    assert result is False
    fake_conn.rollback.assert_called_once()
    fake_conn.close.assert_called_once()


# ---------------------------------------------------------------------------
# main
# ---------------------------------------------------------------------------

def test_main_exits_early_when_no_yf_data(instruments_module, monkeypatch):
    monkeypatch.setattr(instruments_module, "fetch_yfinance_data", lambda tickers: {})
    insert_mock = MagicMock()
    monkeypatch.setattr(instruments_module, "insert_to_db", insert_mock)

    instruments_module.main()

    insert_mock.assert_not_called()


def test_main_also_fetches_db_only_symbols(instruments_module, monkeypatch):
    calls = []

    def fake_fetch(tickers):
        tickers = list(tickers)
        calls.append(tickers)
        if tickers == instruments_module.TICKERS:
            return {"AAPL": {"quoteType": "EQUITY", "shortName": "Apple",
                              "currency": "USD", "fullExchangeName": "NASDAQ"}}
        return {"CUSTOM": {"quoteType": "EQUITY", "shortName": "Custom",
                            "currency": "USD", "fullExchangeName": "NASDAQ"}}

    monkeypatch.setattr(instruments_module, "fetch_yfinance_data", fake_fetch)
    monkeypatch.setattr(instruments_module, "load_instruments", lambda: ["AAPL", "CUSTOM"])
    insert_mock = MagicMock(return_value=True)
    monkeypatch.setattr(instruments_module, "insert_to_db", insert_mock)

    instruments_module.main()

    assert calls == [instruments_module.TICKERS, ["CUSTOM"]]
    inserted_instruments_df = insert_mock.call_args[0][0]
    assert set(inserted_instruments_df["symbol"]) == {"AAPL", "CUSTOM"}
