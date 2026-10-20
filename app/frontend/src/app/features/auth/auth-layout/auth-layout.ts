import { Component, computed, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { Instruments, LatestPrice, LivePrice, dayChange } from '../../../services/instruments';
import { LivePrices } from '../../../services/live-prices';
import { PriceFlash } from '../../../shared/price-flash/price-flash';

export type AuthTab = 'login' | 'register';

const TICKER = ['AAPL', 'MSFT', 'TSLA', 'SPY', 'NVDA', 'BND'];

@Component({
  imports: [RouterLink, PriceFlash],
  selector: 'app-auth-layout',
  styleUrl: './auth-layout.scss',
  templateUrl: './auth-layout.html',
})
export class AuthLayout {
  readonly heading = input.required<string>();
  readonly subheading = input.required<string>();
  readonly activeTab = input.required<AuthTab>();

  private readonly prices = toSignal(
    inject(Instruments)
      .getLatestPrices()
      .pipe(catchError(() => of(new Map<string, LatestPrice>()))),
    { initialValue: new Map<string, LatestPrice>() },
  );
  // While the market is open a live quote replaces the close; the move is still from the previous close.
  private readonly live = toSignal(
    inject(LivePrices)
      .watch(TICKER)
      .pipe(map(({ prices }) => prices)),
    { initialValue: new Map<string, LivePrice>() },
  );

  readonly tickerQuotes = computed(() =>
    TICKER.flatMap((symbol) => {
      const price = this.prices().get(symbol);
      const quote = this.live().get(symbol);
      const current = quote?.price ?? price?.price ?? null;
      const change = price ? dayChange({ price: current, previousClose: price.previousClose }) : null;
      return change
        ? [{ symbol, price: current, up: change.percent >= 0, percent: Math.abs(change.percent).toFixed(2) }]
        : [];
    }),
  );
}
