import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, defer, finalize, of, throwError } from 'rxjs';
import { Auth } from './auth';

export interface Position {
  accountId: string;
  symbol: string;
  quantity: number;
  averageCost: number;
  version?: number;
  createdAt?: string;
  lastUpdated?: string;
  updatedBy?: string;
}

const BASE_URL = '/api/positions';
const NO_ACCOUNT = 'No trading account is linked to this user.';
const LOAD_FAILED = "Couldn't load your positions. Try again in a moment.";

@Injectable({ providedIn: 'root' })
export class Positions {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(Auth);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  getPositions(): Observable<Position[]> {
    return this.track((accountId) =>
      this.http.get<Position[]>(`${BASE_URL}/${encodeURIComponent(accountId)}`),
    );
  }

  getPosition(symbol: string): Observable<Position> {
    return this.track((accountId) =>
      this.http
        .get<Position>(`${BASE_URL}/${encodeURIComponent(accountId)}/${encodeURIComponent(symbol)}`)
        .pipe(
          catchError((err: unknown) =>
            err instanceof HttpErrorResponse && err.status === 404
              ? of<Position>({ accountId, symbol, quantity: 0, averageCost: 0 })
              : throwError(() => err),
          ),
        ),
    );
  }

  private track<T>(request: (accountId: string) => Observable<T>): Observable<T> {
    return defer(() => {
      const accountId = this.auth.currentUser()?.accountId;
      if (!accountId) {
        this.error.set(NO_ACCOUNT);
        return throwError(() => new Error(NO_ACCOUNT));
      }

      this.loading.set(true);
      this.error.set(null);
      return request(accountId).pipe(
        catchError((err: unknown) => {
          this.error.set(messageFor(err));
          return throwError(() => err);
        }),
        finalize(() => this.loading.set(false)),
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
