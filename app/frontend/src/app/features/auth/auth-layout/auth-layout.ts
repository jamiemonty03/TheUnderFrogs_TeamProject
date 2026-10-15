import { Component, computed, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { Instruments, LatestPrice, LivePrice } from '../../../services/instruments';
import { LivePrices } from '../../../services/live-prices';
import { toMarketRows } from '../../instruments/market-overview/market';

export type AuthTab = 'login' | 'register';

export const TICKER_SYMBOLS = ['SPY', 'QQQ', 'AAPL', 'NVDA', 'AMZN', 'GOOGL'];

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

  private readonly closes = toSignal(
    inject(Instruments)
      .getLatestPrices()
      .pipe(catchError(() => of(new Map<string, LatestPrice>()))),
    { initialValue: new Map<string, LatestPrice>() },
  );
  private readonly live = toSignal(
    inject(LivePrices)
      .watch(TICKER_SYMBOLS)
      .pipe(map(({ prices }) => prices)),
    { initialValue: new Map<string, LivePrice>() },
  );

  readonly tickerQuotes = computed(() => {
    const rows = new Map(toMarketRows(this.closes(), this.live()).map((row) => [row.symbol, row]));
    return TICKER_SYMBOLS.flatMap((symbol) => {
      const change = rows.get(symbol)?.change;
      return change
        ? [{ symbol, up: change.percent >= 0, percent: Math.abs(change.percent).toFixed(2) }]
        : [];
    });
  });
}
