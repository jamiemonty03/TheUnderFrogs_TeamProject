export interface Holding {
  symbol: string;
  name: string;
  quantity: number;
  averageCost: number;
  price: number | null;
  priceSource: 'live' | 'close' | null;
  costBasis: number;
  marketValue: number | null;
  unrealisedPnl: number | null;
  pnlPercent: number | null;
  dailyPnl: number | null;
  dailyPnlPercent: number | null;
  dailyCostBasis: number | null;
  weight: number | null;
}

export interface PortfolioSummary {
  invested: number;
  unrealisedPnl: number;
  pnlPercent: number | null;
  dailyPnl: number;
  dailyPnlPercent: number | null;
  positions: number;
  unpriced: number;
}
