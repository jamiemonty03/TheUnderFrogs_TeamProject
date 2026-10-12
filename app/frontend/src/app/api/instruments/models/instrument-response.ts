export interface InstrumentResponse {

  assetClass: 'Equity' | 'Bond' | 'ETF';

  currency: string;

  exchange: string;

  name: string;

  symbol: string;

  tradable: boolean;
}
