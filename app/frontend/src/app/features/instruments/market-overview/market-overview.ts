import { CurrencyPipe, DatePipe, DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { catchError, map, of, switchMap, timer } from 'rxjs';
import { AssetClass, Instruments, LIVE_POLL_MS, LatestPrice } from '../../../services/instruments';
import { MarketRow, filterRows, indexTiles, latestTradeDate, movers, toMarketRows } from './market';

const TABS: { assetClass: AssetClass; label: string }[] = [
  { assetClass: 'Equity', label: 'Stocks' },
  { assetClass: 'ETF', label: 'ETFs' },
  { assetClass: 'Bond', label: 'Bonds' },
];

@Component({
  imports: [CurrencyPipe, DatePipe, DecimalPipe, NgTemplateOutlet, RouterLink, MatButtonModule, MatProgressSpinnerModule],
  selector: 'app-market-overview',
  styleUrl: './market-overview.scss',
  templateUrl: './market-overview.html',
})
export class MarketOverview {
  private readonly instruments = inject(Instruments);

  protected readonly tabs = TABS;
  protected readonly assetClass = signal<AssetClass>('Equity');
  protected readonly query = signal('');

  private readonly prices = signal<ReadonlyMap<string, LatestPrice> | null>(null);
  protected readonly failed = signal(false);

  protected readonly loading = computed(() => this.prices() === null && !this.failed());
  protected readonly allRows = computed(() => toMarketRows(this.prices() ?? new Map()));
  protected readonly rows = computed(() => filterRows(this.allRows(), this.assetClass(), this.query()));
  protected readonly indices = computed(() => indexTiles(this.allRows()));
  protected readonly movers = computed(() => movers(this.allRows()));
  protected readonly asOf = computed(() => latestTradeDate(this.allRows()));

  constructor() {
    timer(0, LIVE_POLL_MS)
      .pipe(
        switchMap(() =>
          this.instruments.getLatestPrices().pipe(
            map((prices): ReadonlyMap<string, LatestPrice> | null => prices),
            catchError(() => of(null)),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((prices) => {
        this.failed.set(prices === null);
        if (prices) {
          this.prices.set(prices);
        }
      });
  }

  protected trend(row: MarketRow | null): 'gain' | 'loss' | null {
    const amount = row?.change?.amount ?? 0;
    return amount > 0 ? 'gain' : amount < 0 ? 'loss' : null;
  }
}
