export interface Account {

  accountId: string;

  active?: boolean;

  cashBalance: number;

  createdAt?: string;

  holderName: string;

  lastUpdated?: string;

  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED';

  updatedBy?: string | null;

  userId?: number;

  version: number;
}
