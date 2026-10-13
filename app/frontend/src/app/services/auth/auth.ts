import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, map, of, switchMap, tap, throwError } from 'rxjs';
import { API_BASE_URL } from '../api-config';

export interface CurrentUser {
  userId: number;
  username: string;
  roles: string[];
  accountId: string | null;
}

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  mfaRequired: boolean;
}

interface StoredSession {
  accessToken: string;
  refreshToken: string;
  user: CurrentUser | null;
}

export const SESSION_KEY = 'auth.session';

@Injectable({ providedIn: 'root' })
export class Auth {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);
  private session: StoredSession | null = readSession();
  private readonly user = signal<CurrentUser | null>(this.session?.user ?? null);

  readonly currentUser = this.user.asReadonly();

  login(username: string, password: string): Observable<CurrentUser> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/auth/login`, { username, password }).pipe(
      tap((tokens) =>
        this.store({
          accessToken: tokens.accessToken,
          refreshToken: tokens.refreshToken,
          user: null,
        }),
      ),
      switchMap(() => this.http.get<CurrentUser>(`${this.baseUrl}/auth/me`)),
      tap((user) => {
        this.store({ ...this.session!, user });
        this.user.set(user);
      }),
      catchError((error: unknown) => {
        this.clearSession();
        return throwError(() => error);
      }),
    );
  }

  logout(): Observable<void> {
    const refreshToken = this.session?.refreshToken;
    if (!refreshToken) {
      this.clearSession();
      return of(undefined);
    }
    return this.http
      .post<void>(`${this.baseUrl}/auth/logout`, { refresh_token: refreshToken })
      .pipe(
        catchError(() => of(undefined)),
        map(() => undefined),
        finalize(() => this.clearSession()),
      );
  }

  isAuthenticated(): boolean {
    const token = this.session?.accessToken;
    return !!token && !isExpired(token);
  }

  getToken(): string | null {
    return this.isAuthenticated() ? this.session!.accessToken : null;
  }

  clearSession(): void {
    this.session = null;
    this.user.set(null);
    try {
      sessionStorage.removeItem(SESSION_KEY);
    } catch {
      return;
    }
  }

  private store(session: StoredSession): void {
    this.session = session;
    try {
      sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
    } catch {
      return;
    }
  }
}

function readSession(): StoredSession | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    return raw ? (JSON.parse(raw) as StoredSession) : null;
  } catch {
    return null;
  }
}

function isExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return typeof payload.exp !== 'number' || payload.exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}
