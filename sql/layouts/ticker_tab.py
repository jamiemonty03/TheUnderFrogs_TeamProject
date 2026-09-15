from dash import dcc, html
import dash_bootstrap_components as dbc


def create_ticker_tab_layout(available_tickers):
    """Create the ticker analysis tab."""
    return dbc.Tab(label="📊 Ticker Analysis", tab_id="ticker-tab", children=[
        dbc.Row([
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.Label("Select Ticker:", className="fw-bold"),
                        dcc.Dropdown(
                            id='ticker-selector',
                            options=[{'label': ticker, 'value': ticker} for ticker in available_tickers],
                            value=available_tickers[0] if available_tickers else None,
                            clearable=False
                        ),
                    ])
                ], className="mb-4")
            ], md=4),
            
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.Label("Date Range:", className="fw-bold"),
                        dcc.DatePickerRange(
                            id='date-range-picker',
                            display_format='YYYY-MM-DD',
                            className="w-100"
                        ),
                    ])
                ], className="mb-4")
            ], md=8),
        ]),
        
        # Key Metrics Row
        dbc.Row([
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H6("Current Price", className="text-muted"),
                        html.H4(id='current-price', children="--")
                    ])
                ])
            ], md=3),
            
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H6("Avg 30D Volatility", className="text-muted"),
                        html.H4(id='avg-volatility', children="--")
                    ])
                ])
            ], md=3),
            
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H6("Momentum Score", className="text-muted"),
                        html.H4(id='momentum-score', children="--")
                    ])
                ])
            ], md=3),
            
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H6("Avg Volume (30D)", className="text-muted"),
                        html.H4(id='avg-volume', children="--")
                    ])
                ])
            ], md=3),
        ], className="mb-4"),
        
        # Price & Moving Averages
        dbc.Row([
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H5("Price Trend with Moving Averages", className="card-title"),
                        dcc.Graph(id='price-trend-chart')
                    ])
                ])
            ], md=12)
        ], className="mb-4"),
        
        # Daily Returns & Momentum
        dbc.Row([
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H5("Growth of $100 Invested", className="card-title"),
                        dcc.Graph(id='returns-histogram')
                    ])
                ])
            ], md=6),
            
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H5("Momentum Score Trend", className="card-title"),
                        dcc.Graph(id='momentum-chart')
                    ])
                ])
            ], md=6),
        ], className="mb-4"),
        
        # Volatility & Volume
        dbc.Row([
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H5("30-Day Volatility Trend", className="card-title"),
                        dcc.Graph(id='volatility-chart')
                    ])
                ])
            ], md=6),
            
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H5("Volume Spike Ratio", className="card-title"),
                        dcc.Graph(id='volume-spike-chart')
                    ])
                ])
            ], md=6),
        ], className="mb-4"),
        
        # Summary Stats
        dbc.Row([
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H5("Performance Summary", className="card-title"),
                        html.Div(id='summary-stats')
                    ])
                ])
            ], md=12)
        ], className="mb-4")
    ])
