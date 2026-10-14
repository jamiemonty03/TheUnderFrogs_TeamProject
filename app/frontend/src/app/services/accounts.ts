import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, defer, throwError } from 'rxjs';
import { Auth } from './auth';

export interface Account {
  accountId: string;
  holderName: string;
  cashBalance: number;
  status: string;
  lastUpdated?: string;
}

@Injectable({ providedIn: 'root' })
export class Accounts {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(Auth);

  getAccount(): Observable<Account> {
    return defer(() => {
      const accountId = this.auth.currentUser()?.accountId;
      if (!accountId) {
        return throwError(() => new Error('No trading account is linked to this user.'));
      }
      return this.http.get<Account>(`/api/accounts/${encodeURIComponent(accountId)}`);
    });
  }
}
