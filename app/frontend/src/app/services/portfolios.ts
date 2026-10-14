import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, defer, finalize, throwError } from 'rxjs';
import { Auth } from './auth';
import { NewPortfolio, Portfolio, PortfolioChanges } from './interfaces/portfolios.interfaces';

const BASE_URL = '/api/portfolios';
const NO_ACCOUNT = 'No trading account is linked to this user.';
const REQUEST_FAILED = "Couldn't update your portfolios. Try again in a moment.";

@Injectable({ providedIn: 'root' })
export class Portfolios {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(Auth);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  getPortfolios(): Observable<Portfolio[]> {
    return this.track((accountId) => this.http.get<Portfolio[]>(accountUrl(accountId)));
  }

  getPortfolio(portfolioId: string): Observable<Portfolio> {
    return this.track((accountId) =>
      this.http.get<Portfolio>(portfolioUrl(accountId, portfolioId)),
    );
  }

  createPortfolio(portfolio: NewPortfolio): Observable<Portfolio> {
    return this.track((accountId) => this.http.post<Portfolio>(accountUrl(accountId), portfolio));
  }

  updatePortfolio(portfolioId: string, changes: PortfolioChanges): Observable<Portfolio> {
    return this.track((accountId) =>
      this.http.patch<Portfolio>(portfolioUrl(accountId, portfolioId), changes),
    );
  }

  deletePortfolio(portfolioId: string): Observable<void> {
    return this.track((accountId) => this.http.delete<void>(portfolioUrl(accountId, portfolioId)));
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

function accountUrl(accountId: string): string {
  return `${BASE_URL}/${encodeURIComponent(accountId)}`;
}

function portfolioUrl(accountId: string, portfolioId: string): string {
  return `${accountUrl(accountId)}/${encodeURIComponent(portfolioId)}`;
}

function messageFor(err: unknown): string {
  if (err instanceof HttpErrorResponse && typeof err.error?.message === 'string') {
    return err.error.message;
  }
  return REQUEST_FAILED;
}
