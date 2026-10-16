import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, defer, map, throwError } from 'rxjs';
import { API_BASE_URL } from './api-config';
import { Auth } from './auth';
import type { TopUpConfirmation, TopUpStart } from './interfaces/wallet.interfaces';

export type { TopUpConfirmation, TopUpStart } from './interfaces/wallet.interfaces';

export class NoTradingAccountError extends Error {
  constructor() {
    super('No trading account is linked to this user.');
    this.name = 'NoTradingAccountError';
  }
}

@Injectable({ providedIn: 'root' })
export class Wallet {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(Auth);
  private readonly baseUrl = inject(API_BASE_URL);

  startTopUp(amount: number, returnTo: string): Observable<string> {
    return this.forAccount((accountUrl) =>
      this.http
        .post<TopUpStart>(`${accountUrl}/top-ups`, { amount, returnTo })
        .pipe(map((response) => response.checkoutUrl)),
    );
  }

  confirmTopUp(sessionId: string): Observable<TopUpConfirmation> {
    return this.forAccount((accountUrl) =>
      this.http.post<TopUpConfirmation>(
        `${accountUrl}/top-ups/${encodeURIComponent(sessionId)}/confirm`,
        {},
      ),
    );
  }

  private forAccount<T>(request: (accountUrl: string) => Observable<T>): Observable<T> {
    return defer(() => {
      const accountId = this.auth.currentUser()?.accountId;
      return accountId
        ? request(`${this.baseUrl}/api/accounts/${encodeURIComponent(accountId)}`)
        : throwError(() => new NoTradingAccountError());
    });
  }
}
