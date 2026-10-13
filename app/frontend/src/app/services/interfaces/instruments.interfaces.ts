export type InstrumentType = 'Stock' | 'ETF' | 'Bond';

export interface BaseInstrumentResponse {
	symbol: string;
	name: string;
	price: number | null;
	tradeDate: string | null;
	createdAt: string | null;
	lastUpdated: string | null;
}

export interface StockResponse extends BaseInstrumentResponse {
	sector: string;
	industry: string;
	country: string;
	fullTimeEmployees: number | null;
	beta: number | null;
	trailingPe: number | null;
	forwardPe: number | null;
	trailingEps: number | null;
	dividendRate: number | null;
	payoutRatio: number | null;
	priceToBook: number | null;
	returnOnEquity: number | null;
	marketCap: number;
	sharesOutstanding: number;
	totalRevenue: number | null;
	website: string | null;
}

export interface EtfResponse extends BaseInstrumentResponse {
	category: string;
	fundFamily: string;
	legalType: string | null;
	netExpenseRatio: number | null;
	navPrice: number;
	totalAssets: number;
	netAssets: number | null;
	ytdReturn: number | null;
	threeYearAvgReturn: number | null;
	fiveYearAvgReturn: number | null;
	beta3Year: number | null;
	distributionYield: number | null;
}

export interface BondResponse extends BaseInstrumentResponse {
	category: string | null;
	fundFamily: string | null;
	legalType: string | null;
	netExpenseRatio: number | null;
	navPrice: number;
	totalAssets: number;
	netAssets: number | null;
	yieldToMaturity: number | null;
	couponRate: number | null;
	duration: number | null;
	beta: number | null;
	ytdReturn: number | null;
	threeYearAvgReturn: number | null;
	fiveYearAvgReturn: number | null;
	distributionYield: number | null;
}

export interface TradingInstrument {
	symbol: string;
	name: string;
	type: InstrumentType;
	marketPrice: number;
	lastUpdated: string;
}