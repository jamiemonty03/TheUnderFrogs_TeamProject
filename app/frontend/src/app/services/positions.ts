import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, throwError } from 'rxjs';
import { Position } from './interfaces/positions.interfaces';
import { AccountApi } from './shared/account-api';

export type { Position } from './interfaces/positions.interfaces';

const BASE_URL = '/api/positions';
const LOAD_FAILED = "Couldn't load your positions. Try again in a moment.";

@Injectable({ providedIn: 'root' })
export class Positions extends AccountApi {
  private readonly http = inject(HttpClient);
  protected readonly failedMessage = LOAD_FAILED;

  getPositions(portfolioId?: string): Observable<Position[]> {
    return this.track((accountId) =>
      this.http.get<Position[]>(`${BASE_URL}/${encodeURIComponent(accountId)}`, {
        params: portfolioParams(portfolioId),
      }),
    );
  }

  getPosition(symbol: string, portfolioId?: string): Observable<Position> {
    return this.track((accountId) =>
      this.http
        .get<Position>(
          `${BASE_URL}/${encodeURIComponent(accountId)}/${encodeURIComponent(symbol)}`,
          {
            params: portfolioParams(portfolioId),
          },
        )
        .pipe(
          catchError((err: unknown) =>
            err instanceof HttpErrorResponse && err.status === 404
              ? of<Position>({
                  accountId,
                  portfolioId: portfolioId ?? null,
                  symbol,
                  quantity: 0,
                  averageCost: 0,
                })
              : throwError(() => err),
          ),
        ),
    );
  }
}

function portfolioParams(portfolioId?: string): Record<string, string> {
  return portfolioId ? { portfolioId } : {};
}
