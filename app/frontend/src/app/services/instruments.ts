import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { EMPTY, Observable, catchError, forkJoin, map, of, scan, switchMap, timer } from 'rxjs';

export type AssetClass = 'Equity' | 'ETF' | 'Bond';

// Matches how often trade-executor's MarketDataPoller asks Alpaca for quotes.
export const LIVE_POLL_MS = 5000;


export interface PricedAsset {
  symbol: string;
  name: string;
  price: number | null;
  previousClose: number | null;
  tradeDate: string | null;
}

export interface LatestPrice {
  symbol: string;
  name: string;
  assetClass: AssetClass;
  price: number | null;
  previousClose: number | null;
  tradeDate: string | null;
}

// Latest live quote from instruments-service's market-data cache; price is the mid of bid and ask.
export interface LivePrice {
  symbol: string;
  price: number;
  bid: number | null;
  ask: number | null;
  quoteTime: string;
}

// What watchLivePrices emits on every poll. A failed poll keeps the last prices and sets stale.
export interface LivePriceFeed {
  prices: ReadonlyMap<string, LivePrice>;
  stale: boolean;
}

export interface DayChange {
  amount: number;
  percent: number;
}

@Injectable({ providedIn: 'root' })
export class Instruments {
  private readonly http = inject(HttpClient);

  getStocks(): Observable<PricedAsset[]> {
    return this.http.get<PricedAsset[]>('/api/stocks');
  }

  getEtfs(): Observable<PricedAsset[]> {
    return this.http.get<PricedAsset[]>('/api/etfs');
  }

  getBonds(): Observable<PricedAsset[]> {
    return this.http.get<PricedAsset[]>('/api/bonds');
  }

  // No symbols asks for every tracked symbol; an empty list asks for nothing.
  // Symbols with no live quote (e.g. outside market hours) are left out of the result.
  getLivePrices(symbols?: string[]): Observable<LivePrice[]> {
    if (symbols?.length === 0) {
      return of([]);
    }
    const params = symbols ? new HttpParams().set('symbols', symbols.join(',')) : undefined;
    return this.http.get<LivePrice[]>('/api/instruments/live-prices', { params });
  }

  // Polls live quotes now and every LIVE_POLL_MS, merging each poll into the prices seen so far.
  // A failed poll keeps the last prices and marks the feed stale until the next one succeeds.
  watchLivePrices(symbols?: string[], intervalMs = LIVE_POLL_MS): Observable<LivePriceFeed> {
    if (symbols?.length === 0) {
      return EMPTY;
    }
    return timer(0, intervalMs).pipe(
      switchMap(() =>
        this.getLivePrices(symbols).pipe(
          map((quotes): LivePrice[] | null => quotes),
          catchError(() => of(null)),
        ),
      ),
      scan(
        (feed: LivePriceFeed, quotes) => {
          if (quotes === null) {
            return { prices: feed.prices, stale: true };
          }
          const prices = new Map(feed.prices);
          quotes.forEach((q) => prices.set(q.symbol, q));
          return { prices, stale: false };
        },
        { prices: new Map<string, LivePrice>(), stale: false },
      ),
    );
  }

  getLatestPrices(): Observable<Map<string, LatestPrice>> {
    return forkJoin({
      Equity: this.getStocks(),
      ETF: this.getEtfs(),
      Bond: this.getBonds(),
    }).pipe(
      map((byClass) => {
        const prices = new Map<string, LatestPrice>();
        for (const assetClass of Object.keys(byClass) as AssetClass[]) {
          for (const asset of byClass[assetClass]) {
            prices.set(asset.symbol, {
              symbol: asset.symbol,
              name: asset.name,
              assetClass,
              price: asset.price,
              previousClose: asset.previousClose,
              tradeDate: asset.tradeDate,
            });
          }
        }
        return prices;
      }),
    );
  }
}

export function dayChange(price: Pick<LatestPrice, 'price' | 'previousClose'>): DayChange | null {
  if (price.price == null || price.previousClose == null || price.previousClose === 0) {
    return null;
  }
  const amount = price.price - price.previousClose;
  return { amount, percent: (amount / price.previousClose) * 100 };
}
