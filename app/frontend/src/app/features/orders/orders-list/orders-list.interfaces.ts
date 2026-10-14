export type OrderStatus = 'NEW' | 'FILLED' | 'REJECTED' | 'CANCELLED';
export type OrderSide = 'BUY' | 'SELL';
export type OrderFilter = 'ALL' | OrderStatus;

export interface OrderRow {
  placed: string;
  symbol: string;
  side: OrderSide;
  quantity: number;
  price: string;
  status: OrderStatus;
}
