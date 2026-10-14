import { HttpErrorResponse } from '@angular/common/http';
import { inject, signal } from '@angular/core';
import { Observable, catchError, defer, finalize, throwError } from 'rxjs';
import { Auth } from '../auth';

const NO_ACCOUNT = 'No trading account is linked to this user.';

/**
 * Shared by services whose URLs need the signed-in user's account ID (Positions, Portfolios).
 * `track()` finds the account ID, and keeps `loading` and `error` up to date for the page.
 */
export abstract class AccountApi {
  private readonly auth = inject(Auth);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  /** Shown when a request fails and the backend didn't send its own message. */
  protected abstract readonly failedMessage: string;

  protected track<T>(request: (accountId: string) => Observable<T>): Observable<T> {
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
          this.error.set(this.messageFor(err));
          return throwError(() => err);
        }),
        finalize(() => this.loading.set(false)),
      );
    });
  }

  private messageFor(err: unknown): string {
    if (err instanceof HttpErrorResponse && typeof err.error?.message === 'string') {
      return err.error.message;
    }
    return this.failedMessage;
  }
}
