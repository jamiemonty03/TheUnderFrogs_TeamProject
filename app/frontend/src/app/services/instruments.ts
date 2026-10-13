import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map } from 'rxjs';
import {
	BaseInstrumentResponse,
	BondResponse,
	EtfResponse,
	InstrumentType,
	StockResponse,
	TradingInstrument,
} from './interfaces/instruments.interfaces';
import type { LatestPrice, AssetClass, DayChange } from './interfaces/instruments.interfaces';

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