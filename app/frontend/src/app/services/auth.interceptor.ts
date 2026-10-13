import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { Auth } from './auth';
import { API_BASE_URL } from './api-config';

const TOKEN_PATHS = ['/auth/me', '/auth/logout'];

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(Auth);
  const router = inject(Router);
  const path = trustedPath(request.url, inject(API_BASE_URL));
  const token = auth.getToken();

  const outgoing =
    token && path !== null && (isApiPath(path) || TOKEN_PATHS.includes(path))
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request;

  return next(outgoing).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        path !== null &&
        isApiPath(path)
      ) {
        auth.clearSession();
        if (!router.url.startsWith('/login')) {
          void router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
        }
      }
      return throwError(() => error);
    }),
  );
};

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
