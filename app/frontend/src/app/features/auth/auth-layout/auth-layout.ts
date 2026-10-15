import { Component, computed, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';
import { Instruments, LatestPrice, dayChange } from '../../../services/instruments';

export type AuthTab = 'login' | 'register';

const TICKER = ['AAPL', 'MSFT', 'TSLA', 'SPY', 'NVDA', 'BND'];

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

  private readonly prices = toSignal(
    inject(Instruments)
      .getLatestPrices()
      .pipe(catchError(() => of(new Map<string, LatestPrice>()))),
    { initialValue: new Map<string, LatestPrice>() },
  );

  readonly tickerQuotes = computed(() =>
    TICKER.flatMap((symbol) => {
      const price = this.prices().get(symbol);
      const change = price ? dayChange(price) : null;
      return change
        ? [{ symbol, up: change.percent >= 0, percent: Math.abs(change.percent).toFixed(2) }]
        : [];
    }),
  );
}
