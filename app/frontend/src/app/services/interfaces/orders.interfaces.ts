export type ApiOrderStatus = 'NEW' | 'FILLED' | 'CANCELLED' | 'REJECTED';
export type ApiOrderSide = 'BUY' | 'SELL';

export interface PlaceOrderRequest {
  accountId: string;
  symbol: string;
  side: ApiOrderSide;
  quantity: number;
  priceLimit: number;
  idempotencyKey: string;
  portfolioId?: string;
}

export interface ClientTradeResponse {
  tradeId: number;
  accountId: string;
  symbol: string;
  tradeType: ApiOrderSide;
  quantity: number;
  price: number;
  tradeDate: string;
  version: number;
  createdAt: string;
  lastUpdated: string | null;
  updatedBy: string | null;
}

export interface OrderResponse {
  orderId: string;
  accountId: string;
  symbol: string;
  side: ApiOrderSide;
  quantity: number;
  priceLimit: number;
  orderStatus: ApiOrderStatus;
  rejectionReason?: string | null;
  version: number;
  createdAt: string;
  lastUpdated: string | null;
  updatedBy: string | null;
}