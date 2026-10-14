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
