from dash import Input, Output, dcc, html
import dash_bootstrap_components as dbc
import plotly.graph_objects as go
import plotly.express as px
from data.loaders import (
    get_live_prices_data,
    get_live_trade_feed_data,
    load_trade_analytics
)
from data.kafka_consumers import is_kafka_available


def register_live_callbacks(app):
    """Register all callbacks for the live data tab."""
    
    @app.callback(
        Output('live-prices-table-container', 'children'),
        Input('live-refresh-interval', 'n_intervals')
    )
    def update_live_prices(n_intervals):
        """Update live prices table."""
        if not is_kafka_available():
            return dbc.Alert(
                "⚠️ No live data available - Kafka is unavailable",
                color="warning",
                className="mb-0"
            )
        
        prices_df = get_live_prices_data()
        
        if prices_df.empty:
            return html.P(
                "Waiting for price updates...",
                className="text-muted"
            )
        
        # Format the dataframe for display
        display_df = prices_df.copy()
        display_df['price'] = display_df['price'].apply(lambda x: f"${x:.2f}")
        display_df['timestamp'] = display_df['timestamp'].dt.strftime('%H:%M:%S')
        display_df = display_df[['symbol', 'price', 'currency', 'timestamp']]
        
        # Create table
        table = dbc.Table.from_dataframe(
            display_df,
            striped=True,
            bordered=True,
            hover=True,
            className="table-sm",
            responsive=True
        )
        
        return html.Div([
            html.Small(f"🟢 {len(prices_df)} symbols updated", className="text-success"),
            table
        ])
    
    @app.callback(
        Output('live-trade-feed-container', 'children'),
        Input('live-refresh-interval', 'n_intervals')
    )
    def update_live_trade_feed(n_intervals):
        """Update live trade feed."""
        if not is_kafka_available():
            return dbc.Alert(
                "⚠️ No live data available - Kafka is unavailable",
                color="warning",
                className="mb-0"
            )
        
        trades_df = get_live_trade_feed_data()
        
        if trades_df.empty:
            return html.P(
                "Waiting for trade events...",
                className="text-muted"
            )
        
        # Show only the last 20 trades
        display_df = trades_df.head(20).copy()
        display_df['timestamp'] = display_df['timestamp'].dt.strftime('%H:%M:%S')
        display_df = display_df[['order_id', 'symbol', 'event_type', 'status', 'quantity', 'price', 'timestamp']]
        
        # Color code by status
        table_rows = []
        for idx, row in display_df.iterrows():
            status_color = 'success' if row['status'] in ['FILLED', 'ACCEPTED'] else 'danger' if row['status'] in ['REJECTED', 'CANCELLED'] else 'secondary'
            row_cells = [
                html.Td(row['order_id'], className="small"),
                html.Td(row['symbol'], className="small fw-bold"),
                html.Td(row['event_type'], className="small"),
                html.Td(row['status'], className=f"small text-{status_color}"),
                html.Td(f"{int(row['quantity'])}", className="small"),
                html.Td(f"${row['price']:.2f}", className="small"),
                html.Td(row['timestamp'], className="small"),
            ]
            table_rows.append(html.Tr(row_cells))
        
        table = html.Table(
            [
                html.Thead(html.Tr([
                    html.Th("Order ID", className="small"),
                    html.Th("Symbol", className="small"),
                    html.Th("Event", className="small"),
                    html.Th("Status", className="small"),
                    html.Th("Qty", className="small"),
                    html.Th("Price", className="small"),
                    html.Th("Time", className="small"),
                ])),
                html.Tbody(table_rows)
            ],
            className="table table-sm table-striped mb-0",
            style={'width': '100%'}
        )
        
        return html.Div([
            html.Small(f"🟢 {len(trades_df)} recent events", className="text-success"),
            table
        ])
    
    @app.callback(
        Output('volume-by-instrument-chart', 'figure'),
        Input('live-refresh-interval', 'n_intervals')
    )
    def update_volume_by_instrument_chart(n_intervals):
        """Update volume by instrument chart."""
        try:
            analytics = load_trade_analytics()
            volume_df = analytics['volume_by_instrument']
            
            if volume_df.empty:
                return {
                    'data': [],
                    'layout': go.Layout(
                        title='No data available',
                        xaxis={'title': 'Instrument'},
                        yaxis={'title': 'Volume'}
                    )
                }
            
            # Sort by total quantity
            volume_df = volume_df.sort_values('total_quantity', ascending=True)
            
            fig = go.Figure(data=[
                go.Bar(
                    y=volume_df['symbol'],
                    x=volume_df['total_quantity'],
                    orientation='h',
                    marker=dict(color='#1f77b4'),
                    hovertemplate='<b>%{y}</b><br>Volume: %{x}<extra></extra>'
                )
            ])
            
            fig.update_layout(
                title='Top 20 Instruments by Total Volume',
                xaxis_title='Total Quantity',
                yaxis_title='Symbol',
                height=400,
                margin=dict(l=100, r=20, t=40, b=20),
                hovermode='closest'
            )
            
            return fig
        except Exception as e:
            print(f"Error creating volume chart: {e}")
            return {
                'data': [],
                'layout': go.Layout(title='Error loading data')
            }
    
    @app.callback(
        Output('fills-vs-rejects-chart', 'figure'),
        Input('live-refresh-interval', 'n_intervals')
    )
    def update_fills_vs_rejects_chart(n_intervals):
        """Update fills vs rejects chart."""
        try:
            analytics = load_trade_analytics()
            status_df = analytics['fills_vs_rejects']
            
            if status_df.empty:
                return {
                    'data': [],
                    'layout': go.Layout(title='No data available')
                }
            
            # Create pie chart
            colors = {
                'FILLED': '#28a745',
                'REJECTED': '#dc3545',
                'CANCELLED': '#ffc107',
                'PENDING': '#6c757d'
            }
            
            fig = go.Figure(data=[
                go.Pie(
                    labels=status_df['status'],
                    values=status_df['count'],
                    marker=dict(
                        colors=[colors.get(status, '#999') for status in status_df['status']]
                    ),
                    hovertemplate='<b>%{label}</b><br>Count: %{value}<extra></extra>'
                )
            ])
            
            fig.update_layout(
                title='Trade Status Distribution',
                height=400,
                margin=dict(l=20, r=20, t=40, b=20)
            )
            
            return fig
        except Exception as e:
            print(f"Error creating status chart: {e}")
            return {
                'data': [],
                'layout': go.Layout(title='Error loading data')
            }
    
    @app.callback(
        Output('daily-volume-trend-chart', 'figure'),
        Input('live-refresh-interval', 'n_intervals')
    )
    def update_daily_volume_trend_chart(n_intervals):
        """Update daily volume trend chart."""
        try:
            analytics = load_trade_analytics()
            daily_df = analytics['daily_volume']
            
            if daily_df.empty:
                return {
                    'data': [],
                    'layout': go.Layout(title='No data available')
                }
            
            # Sort by date
            daily_df = daily_df.sort_values('trade_date')
            
            fig = go.Figure()
            
            fig.add_trace(go.Scatter(
                x=daily_df['trade_date'],
                y=daily_df['total_quantity'],
                mode='lines+markers',
                name='Daily Volume',
                line=dict(color='#1f77b4', width=2),
                marker=dict(size=6),
                hovertemplate='<b>%{x|%Y-%m-%d}</b><br>Volume: %{y}<extra></extra>'
            ))
            
            fig.update_layout(
                title='Daily Trade Volume (Last 30 Days)',
                xaxis_title='Date',
                yaxis_title='Total Quantity',
                height=400,
                margin=dict(l=60, r=20, t=40, b=40),
                hovermode='x unified',
                plot_bgcolor='#f8f9fa'
            )
            
            return fig
        except Exception as e:
            print(f"Error creating daily volume chart: {e}")
            return {
                'data': [],
                'layout': go.Layout(title='Error loading data')
            }
    
    @app.callback(
        Output('kafka-status-indicator', 'children'),
        Input('live-refresh-interval', 'n_intervals')
    )
    def update_kafka_status(n_intervals):
        """Update Kafka connection status indicator."""
        if is_kafka_available():
            return html.P(
                "🟢 Kafka: Connected",
                className="text-success small"
            )
        else:
            return html.P(
                "🔴 Kafka: Unavailable (Dashboard will display historical data only)",
                className="text-danger small"
            )
