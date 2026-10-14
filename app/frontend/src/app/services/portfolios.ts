import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { NewPortfolio, Portfolio, PortfolioChanges } from './interfaces/portfolios.interfaces';
import { AccountApi } from './shared/account-api';

const BASE_URL = '/api/portfolios';
const REQUEST_FAILED = "Couldn't update your portfolios. Try again in a moment.";

@Injectable({ providedIn: 'root' })
export class Portfolios extends AccountApi {
  private readonly http = inject(HttpClient);
  protected readonly failedMessage = REQUEST_FAILED;

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
}

function accountUrl(accountId: string): string {
  return `${BASE_URL}/${encodeURIComponent(accountId)}`;
}

function portfolioUrl(accountId: string, portfolioId: string): string {
  return `${accountUrl(accountId)}/${encodeURIComponent(portfolioId)}`;
}
