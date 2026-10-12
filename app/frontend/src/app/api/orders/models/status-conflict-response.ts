export interface StatusConflictResponse {

  currentStatus: 'NEW' | 'FILLED' | 'CANCELLED' | 'REJECTED';

  errorCode: string;

  message: string;
}
