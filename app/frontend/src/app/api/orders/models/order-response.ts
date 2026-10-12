export interface OrderResponse {

  accountId: string;

  createdAt: string;

  lastUpdated: string | null;

  orderId: string;

  orderStatus: 'NEW' | 'FILLED' | 'CANCELLED' | 'REJECTED';

  priceLimit: number;

  quantity: number;

  side: 'BUY' | 'SELL';

  symbol: string;

  updatedBy?: string | null;

  version: number;
}
