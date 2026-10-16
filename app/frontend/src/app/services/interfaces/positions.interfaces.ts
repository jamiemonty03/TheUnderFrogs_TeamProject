export interface Position {
  accountId: string;
  portfolioId: string | null;
  symbol: string;
  quantity: number;
  averageCost: number;
  version?: number;
  createdAt?: string;
  lastUpdated?: string;
  updatedBy?: string;
}

export interface HoldingRow {
  symbol: string;
  name: string;
  assetClass: string;
  portfolioId: string | null;
  portfolioName: string;
  quantity: number;
  averageCost: number;
  totalCost: number;
  price: number | null;
  priceSource: 'live' | 'close' | null;
  marketValue: number | null;
  pnl: number | null;
  pnlPercent: number | null;
  dayChange: number | null;
  dayPercent: number | null;
  weight: number | null;
}
