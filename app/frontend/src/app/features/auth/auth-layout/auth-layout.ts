import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

export type AuthTab = 'login' | 'register';

const TICKER = [
  { symbol: 'AAPL', change: 1.24 },
  { symbol: 'MSFT', change: 0.88 },
  { symbol: 'TSLA', change: -2.1 },
  { symbol: 'SPY', change: 0.41 },
  { symbol: 'NVDA', change: 3.02 },
  { symbol: 'BND', change: -0.12 },
];

@Component({
  imports: [RouterLink],
  selector: 'app-auth-layout',
  styleUrl: './auth-layout.scss',
  templateUrl: './auth-layout.html',
})
export class AuthLayout {
  readonly heading = input.required<string>();
  readonly subheading = input.required<string>();
  readonly activeTab = input.required<AuthTab>();

  readonly tickerQuotes = TICKER.map(({ symbol, change }) => ({
    symbol,
    up: change >= 0,
    percent: Math.abs(change).toFixed(2),
  }));
}
