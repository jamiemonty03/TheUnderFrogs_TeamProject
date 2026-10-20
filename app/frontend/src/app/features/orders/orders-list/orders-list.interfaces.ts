export type OrderStatus = 'NEW' | 'FILLED' | 'REJECTED' | 'CANCELLED';
export type OrderSide = 'BUY' | 'SELL';
export type OrderFilter = 'ALL' | OrderStatus;

export interface OrderRow {
  id: string;
  placed: string;
  symbol: string;
  side: OrderSide;
  quantity: number;
  price: string;
  // true when price is a live quote (ask for BUY, bid for SELL); false when it's the daily close
  live?: boolean;
  limit: string;
  status: OrderStatus;
}
