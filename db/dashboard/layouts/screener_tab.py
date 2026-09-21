from dash import dcc, html, dash_table
import dash_bootstrap_components as dbc


def create_screener_tab_layout(available_asset_classes, available_currencies):
    """Create the instruments screener tab."""
    return dbc.Tab(label="🔍 Instruments Screener", tab_id="screener-tab", children=[
        dbc.Row([
            dbc.Col([
                html.H3("Find Instruments", className="mt-4 mb-4")
            ])
        ]),
        
        # Filters Row
        dbc.Row([
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.Label("Asset Class:", className="fw-bold"),
                        dcc.Dropdown(
                            id='screener-asset-class-filter',
                            options=[{'label': 'All', 'value': 'all'}] + 
                                    [{'label': ac, 'value': ac} for ac in available_asset_classes],
                            value='all',
                            clearable=False
                        ),
                    ])
                ], className="mb-3")
            ], md=3),
            
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.Label("Currency:", className="fw-bold"),
                        dcc.Dropdown(
                            id='screener-currency-filter',
                            options=[{'label': 'All', 'value': 'all'}] + 
                                    [{'label': c, 'value': c} for c in available_currencies],
                            value='all',
                            clearable=False
                        ),
                    ])
                ], className="mb-3")
            ], md=3),
            
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.Label("YTD Return Range (%):", className="fw-bold"),
                        dcc.RangeSlider(
                            id='screener-ytd-return-filter',
                            min=-100,
                            max=500,
                            step=10,
                            value=[-100, 500],
                            marks={i: f'{i}%' for i in range(-100, 501, 100)},
                            tooltip={"placement": "bottom", "always_visible": True}
                        ),
                    ])
                ], className="mb-3")
            ], md=3),
            
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.Label("Max Drawdown (%):", className="fw-bold"),
                        dcc.RangeSlider(
                            id='screener-drawdown-filter',
                            min=-100,
                            max=0,
                            step=5,
                            value=[-100, 0],
                            marks={i: f'{i}%' for i in range(-100, 1, 25)},
                            tooltip={"placement": "bottom", "always_visible": True}
                        ),
                    ])
                ], className="mb-3")
            ], md=3),
        ]),
        
        # Summary Cards
        dbc.Row([
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H6("Equity", className="text-muted"),
                        html.H4(id='equity-count', children="--"),
                        html.Small("instruments", className="text-muted")
                    ])
                ])
            ], md=2),
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H6("ETF", className="text-muted"),
                        html.H4(id='etf-count', children="--"),
                        html.Small("instruments", className="text-muted")
                    ])
                ])
            ], md=2),
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H6("Bond", className="text-muted"),
                        html.H4(id='bond-count', children="--"),
                        html.Small("instruments", className="text-muted")
                    ])
                ])
            ], md=2),
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H6("Avg YTD Return", className="text-muted"),
                        html.H4(id='avg-ytd-return', children="--")
                    ])
                ])
            ], md=2),
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H6("Avg Drawdown", className="text-muted"),
                        html.H4(id='avg-drawdown', children="--")
                    ])
                ])
            ], md=2),
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H6("Total Instruments", className="text-muted"),
                        html.H4(id='total-count', children="--")
                    ])
                ])
            ], md=2),
        ], className="mb-4"),
        
        # Scatter Plot
        dbc.Row([
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H5("Risk vs Return Analysis", className="card-title"),
                        dcc.Graph(id='screener-scatter-plot')
                    ])
                ])
            ], md=12)
        ], className="mb-4"),
        
        # Data Table
        dbc.Row([
            dbc.Col([
                dbc.Card([
                    dbc.CardBody([
                        html.H5("Instruments", className="card-title mb-3"),
                        dash_table.DataTable(
                            id='screener-table',
                            columns=[
                                {'name': 'Symbol', 'id': 'symbol'},
                                {'name': 'Asset Class', 'id': 'asset_class'},
                                {'name': 'Currency', 'id': 'currency'},
                                {'name': 'Latest Price', 'id': 'latest_price', 'type': 'numeric', 'format': {'specifier': '$.2f'}},
                                {'name': '52w High', 'id': 'price_52w_high', 'type': 'numeric', 'format': {'specifier': '$.2f'}},
                                {'name': '52w Low', 'id': 'price_52w_low', 'type': 'numeric', 'format': {'specifier': '$.2f'}},
                                {'name': 'YTD Return (%)', 'id': 'ytd_return', 'type': 'numeric', 'format': {'specifier': '.2f'}},
                                {'name': '1Y Return (%)', 'id': 'one_year_return', 'type': 'numeric', 'format': {'specifier': '.2f'}},
                                {'name': 'Max Drawdown (%)', 'id': 'max_drawdown', 'type': 'numeric', 'format': {'specifier': '.2f'}},
                                {'name': 'Data Age (days)', 'id': 'days_since_update'},
                            ],
                            data=[],
                            style_cell={
                                'textAlign': 'left',
                                'padding': '10px',
                            },
                            style_header={
                                'backgroundColor': '#f8f9fa',
                                'fontWeight': 'bold',
                                'border': '1px solid #dee2e6'
                            },
                            style_data_conditional=[
                                {
                                    'if': {'column_id': 'ytd_return', 'filter_query': '{ytd_return} > 0'},
                                    'color': 'green',
                                    'fontWeight': 'bold'
                                },
                                {
                                    'if': {'column_id': 'ytd_return', 'filter_query': '{ytd_return} < 0'},
                                    'color': 'red',
                                    'fontWeight': 'bold'
                                },
                                {
                                    'if': {'column_id': 'max_drawdown', 'filter_query': '{max_drawdown} < -20'},
                                    'backgroundColor': '#ffe6e6'
                                },
                            ],
                            sort_action='native',
                            filter_action='native',
                            page_size=20,
                            style_table={'overflowX': 'auto'},
                        )
                    ])
                ])
            ], md=12)
        ], className="mb-4")
    ])
