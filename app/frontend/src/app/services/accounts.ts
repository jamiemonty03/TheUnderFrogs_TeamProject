import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, Signal, WritableSignal, inject, signal } from '@angular/core';
import { Observable, catchError, defer, finalize, map, throwError } from 'rxjs';
import { Auth } from './auth';

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

const BASE_URL = '/api/accounts';
const NO_ACCOUNT = 'No trading account is linked to this user.';
const LOAD_FAILED = "Couldn't load your account. Try again in a moment.";

@Injectable({ providedIn: 'root' })
export class Accounts {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(Auth);

  private readonly accountLoading = signal(false);
  private readonly accountError = signal<string | null>(null);
  private readonly balanceLoading = signal(false);
  private readonly balanceError = signal<string | null>(null);

  readonly account: RequestState = {
    loading: this.accountLoading.asReadonly(),
    error: this.accountError.asReadonly(),
  };
  readonly balance: RequestState = {
    loading: this.balanceLoading.asReadonly(),
    error: this.balanceError.asReadonly(),
  };

  getAccount(): Observable<Account> {
    return this.track(this.accountLoading, this.accountError, (accountId) => this.fetch(accountId));
  }

  getBalance(): Observable<number> {
    return this.track(this.balanceLoading, this.balanceError, (accountId) =>
      this.fetch(accountId).pipe(map((account) => account.cashBalance)),
    );
  }

  private fetch(accountId: string): Observable<Account> {
    return this.http.get<Account>(`${BASE_URL}/${encodeURIComponent(accountId)}`);
  }

  private track<T>(
    loading: WritableSignal<boolean>,
    error: WritableSignal<string | null>,
    request: (accountId: string) => Observable<T>,
  ): Observable<T> {
    return defer(() => {
      const accountId = this.auth.currentUser()?.accountId;
      if (!accountId) {
        error.set(NO_ACCOUNT);
        return throwError(() => new Error(NO_ACCOUNT));
      }

      loading.set(true);
      error.set(null);
      return request(accountId).pipe(
        catchError((err: unknown) => {
          error.set(messageFor(err));
          return throwError(() => err);
        }),
        finalize(() => loading.set(false)),
      );
    });
  }
}

function messageFor(err: unknown): string {
  if (err instanceof HttpErrorResponse && typeof err.error?.message === 'string') {
    return err.error.message;
  }
  return LOAD_FAILED;
}
