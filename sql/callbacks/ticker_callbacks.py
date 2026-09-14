from dash import Input, Output
import plotly.graph_objects as go
from plotly.subplots import make_subplots
import pandas as pd
from dash import html
import dash_bootstrap_components as dbc


def register_ticker_callbacks(app, price_metrics_df):
    """Register all ticker tab callbacks."""
    
    @app.callback(
        [Output('date-range-picker', 'min_date_allowed'),
         Output('date-range-picker', 'max_date_allowed'),
         Output('date-range-picker', 'start_date'),
         Output('date-range-picker', 'end_date')],
        Input('ticker-selector', 'value')
    )
    def update_date_range(selected_ticker):
        """Update date range based on selected ticker."""
        if selected_ticker is None or price_metrics_df.empty:
            return None, None, None, None
        
        ticker_df = price_metrics_df[price_metrics_df['ticker'] == selected_ticker]
        min_date = ticker_df['trade_date'].min()
        max_date = ticker_df['trade_date'].max()
        
        return min_date, max_date, min_date, max_date
    
    @app.callback(
        Output('price-trend-chart', 'figure'),
        [Input('ticker-selector', 'value'),
         Input('date-range-picker', 'start_date'),
         Input('date-range-picker', 'end_date')]
    )
    def update_price_trend(ticker, start_date, end_date):
        """Generate price trend chart."""
        if ticker is None:
            return {}
        
        ticker_df = price_metrics_df[price_metrics_df['ticker'] == ticker].copy()
        
        if start_date and end_date:
            ticker_df = ticker_df[(ticker_df['trade_date'] >= start_date) & 
                                  (ticker_df['trade_date'] <= end_date)]
        
        fig = make_subplots(specs=[[{"secondary_y": True}]])
        
        fig.add_trace(
            go.Scatter(
                x=ticker_df['trade_date'],
                y=ticker_df['close_price'],
                name='Close Price',
                line=dict(color='#1f77b4', width=2),
                hovertemplate='<b>Date:</b> %{x}<br><b>Price:</b> $%{y:.2f}<extra></extra>'
            ),
            secondary_y=False
        )
        
        fig.add_trace(
            go.Scatter(
                x=ticker_df['trade_date'],
                y=ticker_df['moving_avg_20'],
                name='20-Day MA',
                line=dict(color='#ff7f0e', width=1, dash='dash'),
                hovertemplate='<b>20-Day MA:</b> $%{y:.2f}<extra></extra>'
            ),
            secondary_y=False
        )
        
        fig.add_trace(
            go.Scatter(
                x=ticker_df['trade_date'],
                y=ticker_df['moving_avg_50'],
                name='50-Day MA',
                line=dict(color='#2ca02c', width=1, dash='dash'),
                hovertemplate='<b>50-Day MA:</b> $%{y:.2f}<extra></extra>'
            ),
            secondary_y=False
        )
        
        fig.update_layout(
            title=f"{ticker} - Price Trend with Moving Averages",
            xaxis_title="Date",
            yaxis_title="Price ($)",
            hovermode='x unified',
            template='plotly_white',
            height=400
        )
        
        return fig
    
    @app.callback(
        Output('returns-histogram', 'figure'),
        [Input('ticker-selector', 'value'),
         Input('date-range-picker', 'start_date'),
         Input('date-range-picker', 'end_date')]
    )
    def update_returns_histogram(ticker, start_date, end_date):
        """Generate wealth growth chart."""
        if ticker is None:
            return {}
        
        ticker_df = price_metrics_df[price_metrics_df['ticker'] == ticker].copy()
        
        if start_date and end_date:
            ticker_df = ticker_df[(ticker_df['trade_date'] >= start_date) & 
                                  (ticker_df['trade_date'] <= end_date)]
        
        ticker_df = ticker_df.sort_values('trade_date').reset_index(drop=True)
        ticker_df = ticker_df.dropna(subset=['daily_return'])
        
        if ticker_df.empty:
            return {}
        
        ticker_df['cumulative_wealth'] = 100 * (1 + ticker_df['daily_return']).cumprod()
        
        final_value = ticker_df['cumulative_wealth'].iloc[-1]
        total_return_pct = ((final_value - 100) / 100) * 100
        
        fig = go.Figure()
        
        fig.add_trace(
            go.Scatter(
                x=ticker_df['trade_date'],
                y=ticker_df['cumulative_wealth'],
                fill='tozeroy',
                name='Wealth Growth',
                line=dict(color='#636EFA', width=2),
                hovertemplate='<b>Date:</b> %{x}<br><b>$100 Invested:</b> $%{y:.2f}<extra></extra>'
            )
        )
        
        fig.add_hline(y=100, line_dash="dash", line_color="gray", opacity=0.5, annotation_text="Initial Investment")
        
        if total_return_pct >= 0:
            fig.update_traces(fillcolor='rgba(99, 110, 250, 0.2)')
        else:
            fig.update_traces(fillcolor='rgba(239, 85, 59, 0.2)')
        
        fig.update_layout(
            title=f"{ticker} - Growth of $100 Invested ({total_return_pct:+.2f}%)",
            xaxis_title="Date",
            yaxis_title="Portfolio Value ($)",
            hovermode='x unified',
            template='plotly_white',
            height=400,
            yaxis=dict(tickprefix='$')
        )
        
        return fig
    
    @app.callback(
        Output('momentum-chart', 'figure'),
        [Input('ticker-selector', 'value'),
         Input('date-range-picker', 'start_date'),
         Input('date-range-picker', 'end_date')]
    )
    def update_momentum_chart(ticker, start_date, end_date):
        """Generate momentum chart."""
        if ticker is None:
            return {}
        
        ticker_df = price_metrics_df[price_metrics_df['ticker'] == ticker].copy()
        
        if start_date and end_date:
            ticker_df = ticker_df[(ticker_df['trade_date'] >= start_date) & 
                                  (ticker_df['trade_date'] <= end_date)]
        
        fig = go.Figure()
        
        fig.add_trace(
            go.Scatter(
                x=ticker_df['trade_date'],
                y=ticker_df['momentum_score'],
                fill='tozeroy',
                name='Momentum Score',
                line=dict(color='#EF553B'),
                hovertemplate='<b>Date:</b> %{x}<br><b>Momentum:</b> %{y:.2f}<extra></extra>'
            )
        )
        
        fig.add_hline(y=0, line_dash="dash", line_color="gray", opacity=0.5)
        fig.add_hrect(y0=0, y1=100, fillcolor="green", opacity=0.1, annotation_text="Bullish", annotation_position="right")
        fig.add_hrect(y0=-100, y1=0, fillcolor="red", opacity=0.1, annotation_text="Bearish", annotation_position="right")
        
        fig.update_layout(
            title=f"{ticker} - Momentum Score (-100 to 100)",
            xaxis_title="Date",
            yaxis_title="Momentum Score",
            hovermode='x unified',
            template='plotly_white',
            height=400,
            yaxis=dict(range=[-100, 100])
        )
        
        return fig
    
    @app.callback(
        Output('volatility-chart', 'figure'),
        [Input('ticker-selector', 'value'),
         Input('date-range-picker', 'start_date'),
         Input('date-range-picker', 'end_date')]
    )
    def update_volatility_chart(ticker, start_date, end_date):
        """Generate volatility chart."""
        if ticker is None:
            return {}
        
        ticker_df = price_metrics_df[price_metrics_df['ticker'] == ticker].copy()
        
        if start_date and end_date:
            ticker_df = ticker_df[(ticker_df['trade_date'] >= start_date) & 
                                  (ticker_df['trade_date'] <= end_date)]
        
        fig = go.Figure()
        
        fig.add_trace(
            go.Scatter(
                x=ticker_df['trade_date'],
                y=ticker_df['volatility_30d'],
                fill='tozeroy',
                name='Volatility (30D)',
                line=dict(color='#AB63FA'),
                hovertemplate='<b>Date:</b> %{x}<br><b>Volatility:</b> %{y:.4f}<extra></extra>'
            )
        )
        
        fig.update_layout(
            title=f"{ticker} - 30-Day Volatility Trend",
            xaxis_title="Date",
            yaxis_title="Volatility",
            hovermode='x unified',
            template='plotly_white',
            height=400
        )
        
        return fig
    
    @app.callback(
        Output('volume-spike-chart', 'figure'),
        [Input('ticker-selector', 'value'),
         Input('date-range-picker', 'start_date'),
         Input('date-range-picker', 'end_date')]
    )
    def update_volume_spike_chart(ticker, start_date, end_date):
        """Generate volume spike chart."""
        if ticker is None:
            return {}
        
        ticker_df = price_metrics_df[price_metrics_df['ticker'] == ticker].copy()
        
        if start_date and end_date:
            ticker_df = ticker_df[(ticker_df['trade_date'] >= start_date) & 
                                  (ticker_df['trade_date'] <= end_date)]
        
        colors = ['red' if x < 1 else 'green' for x in ticker_df['volume_spike_ratio']]
        
        fig = go.Figure()
        
        fig.add_trace(
            go.Bar(
                x=ticker_df['trade_date'],
                y=ticker_df['volume_spike_ratio'],
                marker=dict(color=colors),
                name='Volume Spike Ratio',
                hovertemplate='<b>Date:</b> %{x}<br><b>Ratio:</b> %{y:.2f}x<extra></extra>'
            )
        )
        
        fig.add_hline(y=1.0, line_dash="dash", line_color="blue", annotation_text="Average")
        
        fig.update_layout(
            title=f"{ticker} - Volume Spike Ratio (vs 30-Day Average)",
            xaxis_title="Date",
            yaxis_title="Volume Ratio",
            hovermode='x unified',
            template='plotly_white',
            height=400,
            showlegend=False
        )
        
        return fig
    
    @app.callback(
        [Output('current-price', 'children'),
         Output('avg-volatility', 'children'),
         Output('momentum-score', 'children'),
         Output('avg-volume', 'children')],
        [Input('ticker-selector', 'value'),
         Input('date-range-picker', 'start_date'),
         Input('date-range-picker', 'end_date')]
    )
    def update_key_metrics(ticker, start_date, end_date):
        """Update key metrics cards."""
        if ticker is None:
            return "--", "--", "--", "--"
        
        ticker_df = price_metrics_df[price_metrics_df['ticker'] == ticker].copy()
        
        if start_date and end_date:
            ticker_df = ticker_df[(ticker_df['trade_date'] >= start_date) & 
                                  (ticker_df['trade_date'] <= end_date)]
        
        if ticker_df.empty:
            return "--", "--", "--", "--"
        
        current_price = f"${ticker_df['close_price'].iloc[-1]:.2f}"
        avg_volatility = f"{ticker_df['volatility_30d'].mean() * 100:.2f}%"
        momentum = f"{ticker_df['momentum_score'].iloc[-1]:.2f}"
        avg_vol = f"{ticker_df['avg_volume_30d'].mean():,.0f}"
        
        return current_price, avg_volatility, momentum, avg_vol
    
    @app.callback(
        Output('summary-stats', 'children'),
        [Input('ticker-selector', 'value'),
         Input('date-range-picker', 'start_date'),
         Input('date-range-picker', 'end_date')]
    )
    def update_summary_stats(ticker, start_date, end_date):
        """Update summary statistics table."""
        if ticker is None:
            return html.P("Select a ticker to view summary statistics")
        
        ticker_df = price_metrics_df[price_metrics_df['ticker'] == ticker].copy()
        
        if start_date and end_date:
            ticker_df = ticker_df[(ticker_df['trade_date'] >= start_date) & 
                                  (ticker_df['trade_date'] <= end_date)]
        
        if ticker_df.empty:
            return html.P("No data available for selected range")
        
        total_return = ((ticker_df['close_price'].iloc[-1] / ticker_df['close_price'].iloc[0]) - 1) * 100
        avg_daily_return = ticker_df['daily_return'].mean() * 100
        max_price = ticker_df['close_price'].max()
        min_price = ticker_df['close_price'].min()
        
        return dbc.Table([
            html.Tbody([
                html.Tr([
                    html.Td("Period Return:", className="fw-bold"),
                    html.Td(f"{total_return:+.2f}%", className="text-end")
                ]),
                html.Tr([
                    html.Td("Avg Daily Return:", className="fw-bold"),
                    html.Td(f"{avg_daily_return:+.4f}%", className="text-end")
                ]),
                html.Tr([
                    html.Td("Max Price:", className="fw-bold"),
                    html.Td(f"${max_price:.2f}", className="text-end")
                ]),
                html.Tr([
                    html.Td("Min Price:", className="fw-bold"),
                    html.Td(f"${min_price:.2f}", className="text-end")
                ]),
                html.Tr([
                    html.Td("Price Range:", className="fw-bold"),
                    html.Td(f"${max_price - min_price:.2f}", className="text-end")
                ]),
            ])
        ], hover=True, className="mb-0")
