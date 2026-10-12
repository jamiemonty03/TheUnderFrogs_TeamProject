export interface CreateAccountRequest {

  accountId: string;

  cashBalance: number;

  holderName: string;

  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
}
