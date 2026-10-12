export interface UpdateOrderStatusRequest {

  expectedStatus: 'NEW' | 'FILLED' | 'CANCELLED' | 'REJECTED';

  fillPrice?: number | null;

  newStatus: 'NEW' | 'FILLED' | 'CANCELLED' | 'REJECTED';

  reason?: string | null;
}
