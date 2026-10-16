import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { Auth } from './auth';
import { API_BASE_URL } from '../api-config';

const PROFILE_PATHS = ['/auth/profile', '/auth/password'];
const TOKEN_PATHS = ['/auth/me', '/auth/logout', ...PROFILE_PATHS];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(Auth);
  const router = inject(Router);
  const path = trustedPath(request.url, inject(API_BASE_URL));
  const needsToken = path !== null && (isApiPath(path) || TOKEN_PATHS.includes(path));
  const canRefresh = path !== null && (isApiPath(path) || PROFILE_PATHS.includes(path));

  const withToken = (token: string | null): HttpRequest<unknown> =>
    token && needsToken
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request;

  const sendToLogin = () => {
    auth.clearSession();
    if (!router.url.startsWith('/login')) {
      void router.navigate(['/login'], {
        queryParams: { returnUrl: router.url, expired: 1 },
      });
    }
  };

  const signOutOn401 = (error: unknown) => {
    if (isUnauthorized(error)) {
      sendToLogin();
    }
    return throwError(() => error);
  };

  const sentToken = auth.getToken();

  return next(withToken(sentToken)).pipe(
    catchError((error: unknown) => {
      if (!canRefresh || !isUnauthorized(error)) {
        return throwError(() => error);
      }
      const currentToken = auth.getToken();
      if (currentToken && currentToken !== sentToken) {
        return next(withToken(currentToken)).pipe(catchError(signOutOn401));
      }
      return auth.refresh().pipe(
        catchError(() => {
          sendToLogin();
          return throwError(() => error);
        }),
        switchMap((token) => next(withToken(token)).pipe(catchError(signOutOn401))),
      );
    }),
  );
};

function isUnauthorized(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === 401;
}

function isApiPath(path: string): boolean {
  return path === '/api' || path.startsWith('/api/');
}

function trustedPath(url: string, apiBaseUrl: string): string | null {
  try {
    const appOrigin = window.location.origin;
    const parsed = new URL(url, appOrigin);
    const apiOrigin = apiBaseUrl ? new URL(apiBaseUrl, appOrigin).origin : appOrigin;
    return parsed.origin === appOrigin || parsed.origin === apiOrigin ? parsed.pathname : null;
  } catch {
    return null;
  }
}
