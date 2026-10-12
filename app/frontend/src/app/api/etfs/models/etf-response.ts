export interface EtfResponse {
  beta3Year?: number | null;
  category: string;
  createdAt?: string;
  distributionYield?: number | null;
  fiveYearAvgReturn?: number | null;
  fundFamily: string;
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
  ytdReturn?: number | null;
}
