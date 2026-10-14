import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { EMPTY, Observable, catchError, forkJoin, map, of, scan, switchMap, timer } from 'rxjs';
import {
	BaseInstrumentResponse,
	BondResponse,
	EtfResponse,
	InstrumentType,
	StockResponse,
	TradingInstrument,
} from './interfaces/instruments.interfaces';
import type { LatestPrice, AssetClass, DayChange } from './interfaces/instruments.interfaces';

export type { LatestPrice, DayChange } from './interfaces/instruments.interfaces';
export type PricedAsset = Pick<LatestPrice, 'symbol' | 'name' | 'price' | 'previousClose' | 'tradeDate'>;

export const LIVE_POLL_MS = 5000;

export interface LivePrice {
  symbol: string;
  price: number;
  bid: number | null;
  ask: number | null;
  quoteTime: string;
}

export interface LivePriceFeed {
  prices: ReadonlyMap<string, LivePrice>;
  stale: boolean;
}

@Injectable({ providedIn: 'root' })
export class Instruments {
	constructor(private readonly http: HttpClient) {}

	getStocks(): Observable<StockResponse[]> {
		return this.http.get<StockResponse[]>('/api/stocks');
	}

	getEtfs(): Observable<EtfResponse[]> {
		return this.http.get<EtfResponse[]>('/api/etfs');
	}

	getBonds(): Observable<BondResponse[]> {
		return this.http.get<BondResponse[]>('/api/bonds');
	}

  getLivePrices(symbols?: string[]): Observable<LivePrice[]> {
    if (symbols?.length === 0) {
      return of([]);
    }
    const params = symbols ? new HttpParams().set('symbols', symbols.join(',')) : undefined;
    return this.http.get<LivePrice[]>('/api/instruments/live-prices', { params });
  }

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

	getAllTradableInstruments(): Observable<TradingInstrument[]> {
		return forkJoin({
			stocks: this.getStocks(),
			etfs: this.getEtfs(),
			bonds: this.getBonds(),
		}).pipe(
			map(({ stocks, etfs, bonds }) => {
				const fromStocks = stocks.map((item) => this.toTradingInstrument(item, 'Stock'));
				const fromEtfs = etfs.map((item) => this.toTradingInstrument(item, 'ETF'));
				const fromBonds = bonds.map((item) => this.toTradingInstrument(item, 'Bond'));

				return [...fromStocks, ...fromEtfs, ...fromBonds]
					.filter((item) => item.marketPrice > 0)
					.sort((a, b) => a.symbol.localeCompare(b.symbol));
			}),
		);
	}

	private toTradingInstrument(item: BaseInstrumentResponse, type: InstrumentType): TradingInstrument {
		return {
			symbol: item.symbol,
			name: item.name,
			type,
			marketPrice: Number(item.price ?? 0),
			lastUpdated: item.lastUpdated ?? 'N/A',
		};
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