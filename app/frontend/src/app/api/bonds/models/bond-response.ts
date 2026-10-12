export interface BondResponse {

  beta?: number | null;
  category?: string | null;

  couponRate?: number | null;
  createdAt?: string;
  distributionYield?: number | null;

  duration?: number | null;
  fiveYearAvgReturn?: number | null;
  fundFamily?: string | null;
  lastUpdated?: string;
  legalType?: string | null;
  name: string;
  navPrice: number;
  netAssets?: number | null;
  netExpenseRatio?: number | null;
  price?: number | null;
  symbol: string;
  threeYearAvgReturn?: number | null;
  totalAssets: number;
  tradeDate?: string | null;

  yieldToMaturity?: number | null;
  ytdReturn?: number | null;
}
