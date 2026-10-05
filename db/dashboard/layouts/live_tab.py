from dash import dcc, html
import dash_bootstrap_components as dbc


def create_live_tab_layout():
    """Create the live data and analytics tab."""
    return dbc.Tab(label="🔴 Live & Analytics", tab_id="live-tab", children=[
        # Auto-refresh interval (every 2 seconds)
        dcc.Interval(
            id='live-refresh-interval',
            interval=2000,  # milliseconds
            n_intervals=0
        ),
        
        dbc.Container([
            # LIVE DATA SECTION
            dbc.Row([
                dbc.Col([
                    html.H4("🔴 Live Data", className="mt-4 mb-3")
                ], width=12)
            ]),
            
            # Live Prices and Trades in two columns
            dbc.Row([
                # Live Prices Column
                dbc.Col([
                    dbc.Card([
                        dbc.CardBody([
                            html.H6("📊 Latest Prices from market-data", className="card-title"),
                            html.Div(
                                id='live-prices-table-container',
                                children=html.P(
                                    "Loading live prices...",
                                    className="text-muted"
                                )
                            )
                        ])
                    ], className="mb-4")
                ], md=6),
                
                # Live Trade Feed Column
                dbc.Col([
                    dbc.Card([
                        dbc.CardBody([
                            html.H6("💹 Trade Feed from trade-events", className="card-title"),
                            html.Div(
                                id='live-trade-feed-container',
                                children=html.P(
                                    "Loading trade feed...",
                                    className="text-muted"
                                )
                            )
                        ])
                    ], className="mb-4")
                ], md=6),
            ]),
            
            # ANALYTICS SECTION
            dbc.Row([
                dbc.Col([
                    html.H4("📈 Trade Analytics", className="mt-4 mb-3")
                ], width=12)
            ]),
            
            # Analytics Charts
            dbc.Row([
                # Volume by Instrument
                dbc.Col([
                    dbc.Card([
                        dbc.CardBody([
                            html.H6("Top Instruments by Volume", className="card-title"),
                            dcc.Loading(
                                id="loading-volume-chart",
                                type="default",
                                children=[
                                    dcc.Graph(id='volume-by-instrument-chart')
                                ]
                            )
                        ])
                    ], className="mb-4")
                ], md=6),
                
                # Fills vs Rejects
                dbc.Col([
                    dbc.Card([
                        dbc.CardBody([
                            html.H6("Trade Status Distribution", className="card-title"),
                            dcc.Loading(
                                id="loading-status-chart",
                                type="default",
                                children=[
                                    dcc.Graph(id='fills-vs-rejects-chart')
                                ]
                            )
                        ])
                    ], className="mb-4")
                ], md=6),
            ]),
            
            # Daily Volume Trend
            dbc.Row([
                dbc.Col([
                    dbc.Card([
                        dbc.CardBody([
                            html.H6("Daily Volume Trend (Last 30 Days)", className="card-title"),
                            dcc.Loading(
                                id="loading-daily-chart",
                                type="default",
                                children=[
                                    dcc.Graph(id='daily-volume-trend-chart')
                                ]
                            )
                        ])
                    ], className="mb-4")
                ], md=12)
            ]),
            
            # Status indicator
            dbc.Row([
                dbc.Col([
                    html.Div(
                        id='kafka-status-indicator',
                        className="text-center mb-4",
                        children=html.P("Status: Checking...", className="text-muted small")
                    )
                ], width=12)
            ]),
        ], fluid=True)
    ])
