import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';

import {
	BaseInstrumentResponse,
	BondResponse,
	EtfResponse,
	InstrumentSummary,
	InstrumentType,
	PricePoint,
 	LivePrice,
  	LivePriceFeed,
	StockResponse,
	TradingInstrument,
} from './interfaces/instruments.interfaces';
import type { LatestPrice, AssetClass, DayChange } from './interfaces/instruments.interfaces';

export type {
  AssetClass,
  DayChange,
  LatestPrice,
  LivePrice,
  LivePriceFeed,
  PricedAsset,
} from './interfaces/instruments.interfaces';

export const LIVE_POLL_MS = 30_000;

export interface NewsArticle {
	headline: string;
	articleUrl: string;
	publishedAt: string | null;
	updatedAt: string | null;
	symbols: string[];
}

@Injectable({ providedIn: 'root' })
export class Instruments {
	private readonly http = inject(HttpClient);

	getStocks(): Observable<StockResponse[]> {
		return this.http.get<StockResponse[]>('/api/stocks');
	}

	getEtfs(): Observable<EtfResponse[]> {
		return this.http.get<EtfResponse[]>('/api/etfs');
	}

	getBonds(): Observable<BondResponse[]> {
		return this.http.get<BondResponse[]>('/api/bonds');
	}

	getInstrument(symbol: string): Observable<InstrumentSummary> {
		return this.http.get<InstrumentSummary>(`/api/instruments/${encodeURIComponent(symbol)}`);
	}

	getHistory(symbol: string, days = 252): Observable<PricePoint[]> {
		return this.http.get<PricePoint[]>(`/api/instruments/${encodeURIComponent(symbol)}/history`, {
			params: { days },
		});
	}

	getStock(symbol: string): Observable<StockResponse> {
		return this.http.get<StockResponse>(`/api/stocks/${encodeURIComponent(symbol)}`);
	}

	getEtf(symbol: string): Observable<EtfResponse> {
		return this.http.get<EtfResponse>(`/api/etfs/${encodeURIComponent(symbol)}`);
	}

	getBond(symbol: string): Observable<BondResponse> {
		return this.http.get<BondResponse>(`/api/bonds/${encodeURIComponent(symbol)}`);
	}

	getNews(limit = 5, symbol?: string): Observable<NewsArticle[]> {
		const params: Record<string, string | number> = symbol ? { limit, symbol } : { limit };
		return this.http.get<NewsArticle[]>('/api/news', { params });
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