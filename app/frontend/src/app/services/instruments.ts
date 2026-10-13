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
}
