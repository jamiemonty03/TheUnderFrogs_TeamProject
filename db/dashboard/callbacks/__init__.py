from .ticker_callbacks import register_ticker_callbacks
from .screener_callbacks import register_screener_callbacks


def register_all_callbacks(app, price_metrics_df, instruments_metrics_df):
    """Register all callbacks for the dashboard."""
    register_ticker_callbacks(app, price_metrics_df)
    register_screener_callbacks(app, instruments_metrics_df)


__all__ = ['register_all_callbacks']
