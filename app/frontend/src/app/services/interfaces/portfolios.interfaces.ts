export type PortfolioRisk = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Portfolio {
  portfolioId: string;
  accountId: string;
  name: string;
  risk: PortfolioRisk;
  benchmark: string | null;
  defaultPortfolio: boolean;
  version?: number;
  createdAt?: string;
  lastUpdated?: string;
  updatedBy?: string;
}

export interface NewPortfolio {
  name: string;
  risk: PortfolioRisk;
  benchmark?: string;
}

export type PortfolioChanges = Partial<NewPortfolio>;
