import { Signal } from '@angular/core';

export interface Account {
  accountId: string;
  holderName: string;
  cashBalance: number;
  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
  version?: number;
  createdAt?: string;
  lastUpdated?: string;
  updatedBy?: string | null;
}

export interface RequestState {
  readonly loading: Signal<boolean>;
  readonly error: Signal<string | null>;
}
