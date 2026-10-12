import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';

export type AssetClass = 'Equity' | 'ETF' | 'Bond';

/** The price fields shared by /api/stocks, /api/etfs and /api/bonds. */
export interface PricedAsset {
  symbol: string;
  name: string;
  /** Latest close. Null if the instrument has no price history yet. */
  price: number | null;
  /** Close from the trading day before tradeDate. Null with fewer than two days of history. */
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

export interface DayChange {
  amount: number;
  percent: number;
}

@Injectable()
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

  /**
   * Latest price and previous close for every instrument, keyed by symbol.
   * Used for market value, day P&L and prefilling the order price.
   */
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

/** Change since the previous close, or null when either close is missing. */
export function dayChange(price: Pick<LatestPrice, 'price' | 'previousClose'>): DayChange | null {
  if (price.price == null || price.previousClose == null || price.previousClose === 0) {
    return null;
  }
  const amount = price.price - price.previousClose;
  return { amount, percent: (amount / price.previousClose) * 100 };
}
