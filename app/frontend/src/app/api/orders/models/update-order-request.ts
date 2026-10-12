export interface UpdateOrderRequest {

  orderStatus?: ('NEW' | 'FILLED' | 'CANCELLED' | 'REJECTED') | null;

  priceLimit?: number;

  quantity?: number;

  side?: 'BUY' | 'SELL';

  updatedBy?: string;
}
