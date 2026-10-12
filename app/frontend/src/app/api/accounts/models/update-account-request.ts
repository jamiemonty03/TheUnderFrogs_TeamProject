export interface UpdateAccountRequest {

  holderName?: string | null;

  status?: ('ACTIVE' | 'SUSPENDED' | 'CLOSED') | null;
}
