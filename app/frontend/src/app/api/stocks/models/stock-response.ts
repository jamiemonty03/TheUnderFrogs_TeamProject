export interface StockResponse {

  beta?: number | null;
  country: string;
  createdAt?: string;
  dividendRate?: number | null;
  forwardPe?: number | null;
  fullTimeEmployees?: number | null;
  industry: string;
  lastUpdated?: string;
  marketCap: number;
  name: string;
  payoutRatio?: number | null;

  price?: number | null;
  priceToBook?: number | null;
  returnOnEquity?: number | null;
  sector: string;
  sharesOutstanding: number;
  symbol: string;
  totalRevenue?: number | null;

  tradeDate?: string | null;
  trailingEps?: number | null;
  trailingPe?: number | null;
  website?: string | null;
}
