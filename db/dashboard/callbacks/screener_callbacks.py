from dash import Input, Output
import plotly.graph_objects as go


def register_screener_callbacks(app, instruments_metrics_df):
    """Register all screener tab callbacks."""
    
    @app.callback(
        [Output('equity-count', 'children'),
         Output('etf-count', 'children'),
         Output('bond-count', 'children'),
         Output('avg-ytd-return', 'children'),
         Output('avg-drawdown', 'children'),
         Output('total-count', 'children')],
        [Input('screener-asset-class-filter', 'value'),
         Input('screener-currency-filter', 'value'),
         Input('screener-ytd-return-filter', 'value'),
         Input('screener-drawdown-filter', 'value')]
    )
    def update_summary_cards(asset_class, currency, ytd_return_range, drawdown_range):
        """Update summary statistics cards."""
        if instruments_metrics_df.empty:
            return "--", "--", "--", "--", "--", "--"
        
        # Apply filters
        filtered_df = instruments_metrics_df.copy()
        
        if asset_class != 'all':
            filtered_df = filtered_df[filtered_df['asset_class'] == asset_class]
        
        if currency != 'all':
            filtered_df = filtered_df[filtered_df['currency'] == currency]
        
        if ytd_return_range:
            filtered_df = filtered_df[
                (filtered_df['ytd_return'] >= ytd_return_range[0]) &
                (filtered_df['ytd_return'] <= ytd_return_range[1])
            ]
        
        if drawdown_range:
            filtered_df = filtered_df[
                (filtered_df['max_drawdown'] >= drawdown_range[0]) &
                (filtered_df['max_drawdown'] <= drawdown_range[1])
            ]
        
        # Calculate stats on full dataset for counts, filtered for averages
        equity_count = len(instruments_metrics_df[instruments_metrics_df['asset_class'] == 'Equity'])
        etf_count = len(instruments_metrics_df[instruments_metrics_df['asset_class'] == 'ETF'])
        bond_count = len(instruments_metrics_df[instruments_metrics_df['asset_class'] == 'Bond'])
        
        avg_ytd = filtered_df['ytd_return'].mean() if not filtered_df.empty else 0
        avg_dd = filtered_df['max_drawdown'].mean() if not filtered_df.empty else 0
        total = len(instruments_metrics_df)
        
        return (
            f"{equity_count}",
            f"{etf_count}",
            f"{bond_count}",
            f"{avg_ytd:+.2f}%",
            f"{avg_dd:.2f}%",
            f"{total}"
        )
    
    @app.callback(
        Output('screener-scatter-plot', 'figure'),
        [Input('screener-asset-class-filter', 'value'),
         Input('screener-currency-filter', 'value'),
         Input('screener-ytd-return-filter', 'value'),
         Input('screener-drawdown-filter', 'value')]
    )
    def update_scatter_plot(asset_class, currency, ytd_return_range, drawdown_range):
        """Generate risk vs return scatter plot."""
        if instruments_metrics_df.empty:
            return {}
        
        # Apply filters
        filtered_df = instruments_metrics_df.copy()
        
        if asset_class != 'all':
            filtered_df = filtered_df[filtered_df['asset_class'] == asset_class]
        
        if currency != 'all':
            filtered_df = filtered_df[filtered_df['currency'] == currency]
        
        if ytd_return_range:
            filtered_df = filtered_df[
                (filtered_df['ytd_return'] >= ytd_return_range[0]) &
                (filtered_df['ytd_return'] <= ytd_return_range[1])
            ]
        
        if drawdown_range:
            filtered_df = filtered_df[
                (filtered_df['max_drawdown'] >= drawdown_range[0]) &
                (filtered_df['max_drawdown'] <= drawdown_range[1])
            ]
        
        # Create color mapping by asset class
        color_map = {'Equity': '#1f77b4', 'ETF': '#ff7f0e', 'Bond': '#2ca02c'}
        colors = [color_map.get(ac, '#999999') for ac in filtered_df['asset_class']]
        
        fig = go.Figure()
        
        # Add scatter points grouped by asset class
        for asset_class_name in ['Equity', 'ETF', 'Bond']:
            class_df = filtered_df[filtered_df['asset_class'] == asset_class_name]
            if class_df.empty:
                continue
            
            fig.add_trace(
                go.Scatter(
                    x=class_df['ytd_return'],
                    y=class_df['max_drawdown'],
                    mode='markers',
                    name=asset_class_name,
                    marker=dict(
                        size=10,
                        color=color_map[asset_class_name],
                        opacity=0.7,
                        line=dict(width=1, color='white')
                    ),
                    text=class_df['symbol'],
                    hovertemplate='<b>%{text}</b><br>YTD Return: %{x:.2f}%<br>Max Drawdown: %{y:.2f}%<extra></extra>'
                )
            )
        
        fig.update_layout(
            title="Risk vs Return: YTD Return vs Max Drawdown",
            xaxis_title="YTD Return (%)",
            yaxis_title="Max Drawdown (%)",
            hovermode='closest',
            template='plotly_white',
            height=400,
            xaxis=dict(zeroline=True, zerolinewidth=2, zerolinecolor='LightGray'),
            yaxis=dict(zeroline=True, zerolinewidth=2, zerolinecolor='LightGray'),
        )
        
        return fig
    
    @app.callback(
        Output('screener-table', 'data'),
        [Input('screener-asset-class-filter', 'value'),
         Input('screener-currency-filter', 'value'),
         Input('screener-ytd-return-filter', 'value'),
         Input('screener-drawdown-filter', 'value')]
    )
    def update_screener_table(asset_class, currency, ytd_return_range, drawdown_range):
        """Update screener table based on filters."""
        if instruments_metrics_df.empty:
            return []
        
        filtered_df = instruments_metrics_df.copy()
        
        # Filter by asset class
        if asset_class != 'all':
            filtered_df = filtered_df[filtered_df['asset_class'] == asset_class]
        
        # Filter by currency
        if currency != 'all':
            filtered_df = filtered_df[filtered_df['currency'] == currency]
        
        # Filter by YTD return range
        if ytd_return_range:
            filtered_df = filtered_df[
                (filtered_df['ytd_return'] >= ytd_return_range[0]) &
                (filtered_df['ytd_return'] <= ytd_return_range[1])
            ]
        
        # Filter by max drawdown range
        if drawdown_range:
            filtered_df = filtered_df[
                (filtered_df['max_drawdown'] >= drawdown_range[0]) &
                (filtered_df['max_drawdown'] <= drawdown_range[1])
            ]
        
        # Sort by YTD return (descending)
        filtered_df = filtered_df.sort_values('ytd_return', ascending=False)
        
        return filtered_df.to_dict('records')
