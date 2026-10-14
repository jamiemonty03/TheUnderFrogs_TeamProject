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

export interface OrderResponse {
  orderId: string;
  accountId: string;
  symbol: string;
  side: ApiOrderSide;
  quantity: number;
  priceLimit: number;
  orderStatus: ApiOrderStatus;
  version: number;
  createdAt: string;
  lastUpdated: string | null;
  updatedBy: string | null;
}