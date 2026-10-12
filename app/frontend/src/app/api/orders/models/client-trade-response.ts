export interface ClientTradeResponse {

  accountId: string;

  createdAt: string;

  lastUpdated: string;

  price: number;

  quantity: number;

  symbol: string;

  tradeDate: string;

  tradeId: number;

  tradeType: 'BUY' | 'SELL';

  updatedBy: string;

  version: number;
}
