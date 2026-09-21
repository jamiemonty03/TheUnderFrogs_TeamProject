import dash
from dash import html
import dash_bootstrap_components as dbc

from data.loaders import (
    load_price_metrics,
    load_instruments_metrics,
    get_available_tickers,
    get_available_asset_classes,
    get_available_currencies
)
from layouts import create_ticker_tab_layout, create_screener_tab_layout
from callbacks import register_all_callbacks


# Initialize Dash app
app = dash.Dash(__name__, external_stylesheets=[dbc.themes.BOOTSTRAP])

# Load data
price_metrics_df = load_price_metrics()
instruments_metrics_df = load_instruments_metrics()

# Get available options
available_tickers = get_available_tickers(price_metrics_df)
available_asset_classes = get_available_asset_classes(instruments_metrics_df)
available_currencies = get_available_currencies(instruments_metrics_df)

# Create app layout
app.layout = dbc.Container([
    dbc.Row([
        dbc.Col([
            html.H1("📈 Financial Analytics Dashboard", className="mb-4 mt-4 text-center")
        ])
    ]),
    
    dbc.Tabs([
        create_ticker_tab_layout(available_tickers),
        create_screener_tab_layout(available_asset_classes, available_currencies),
    ], id="dashboard-tabs", active_tab="ticker-tab")
], fluid=True, className="bg-light")

# Register all callbacks
register_all_callbacks(app, price_metrics_df, instruments_metrics_df)


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=8080)

