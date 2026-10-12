export interface PlaceOrderRequest {

  accountId: string;

  idempotencyKey: string;

  priceLimit: number;

  quantity: number;

  side: 'BUY' | 'SELL';

  symbol: string;
}
