import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Component } from '@angular/core';
import { authInterceptor } from './auth.interceptor';
import { Auth, SESSION_KEY } from './auth';
import { CurrentUser } from '../interfaces/auth.interfaces';
import { API_BASE_URL } from '../api-config';

@Component({ template: '' })
class Blank {}

function jwt(expiresInSeconds: number): string {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'RS256' })}.${encode({ sub: '11', exp: Math.floor(Date.now() / 1000) + expiresInSeconds })}.signature`;
}

const demo: CurrentUser = { userId: 11, username: 'demo', roles: ['TRADER'], accountId: 'ACC0011' };

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpController: HttpTestingController;
  let router: Router;
  let token: string | null;
  const clearSession = vi.fn();
  const refresh = vi.fn();

  beforeEach(() => {
    token = 'access.jwt.token';
    clearSession.mockReset();
    refresh.mockReset().mockReturnValue(throwError(() => new Error('refresh failed')));
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'orders', component: Blank },
          { path: 'login', component: Blank },
        ]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Auth, useValue: { getToken: () => token, clearSession, refresh } },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpController = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => httpController.verify());

  function authorizationFor(url: string, method: 'GET' | 'POST' = 'GET'): string | null {
    const call = method === 'GET' ? http.get(url) : http.post(url, {});
    call.subscribe({ error: () => undefined });
    const request = httpController.expectOne(url);
    request.flush({});
    return request.request.headers.get('Authorization');
  }

  it.each([
    '/api/accounts/ACC0011',
    '/api/orders/account/ACC0011',
    '/api/positions/ACC0011?view=summary',
    `${window.location.origin}/api/instruments`,
    '/auth/me',
  ])('adds the Bearer token to %s', (url) => {
    expect(authorizationFor(url)).toBe('Bearer access.jwt.token');
  });

  it('adds the Bearer token to /auth/logout', () => {
    expect(authorizationFor('/auth/logout', 'POST')).toBe('Bearer access.jwt.token');
  });

  it.each([
    ['login', '/auth/login', 'POST'],
    ['register', '/auth/register', 'POST'],
    ['refresh', '/auth/refresh', 'POST'],
    ['another origin', 'https://evil.example.com/api/accounts/ACC0011', 'GET'],
    ['a protocol-relative URL to another host', '//evil.example.com/api/accounts', 'GET'],
    ['a path that only starts with "api"', '/apikeys', 'GET'],
    ['a static asset', '/assets/logo.svg', 'GET'],
  ] as const)('leaves the header off for %s', (_label, url, method) => {
    expect(authorizationFor(url, method)).toBeNull();
  });

  it('sends no header when the user is not logged in', () => {
    token = null;

    expect(authorizationFor('/api/accounts/ACC0011')).toBeNull();
  });

  describe('when an API call returns 401 and the refresh fails', () => {
    it('clears the session and redirects to /login with the returnUrl and the expired flag', async () => {
      await router.navigateByUrl('/orders?status=NEW');
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

      const result = firstValueFrom(http.get('/api/orders/account/ACC0011'));
      httpController
        .expectOne('/api/orders/account/ACC0011')
        .flush(null, { status: 401, statusText: 'Unauthorized' });

      await expect(result).rejects.toMatchObject({ status: 401 });
      expect(refresh).toHaveBeenCalledTimes(1);
      expect(clearSession).toHaveBeenCalledTimes(1);
      expect(navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/orders?status=NEW', expired: 1 },
      });
    });

    it('does not redirect again when the user is already on the login page', async () => {
      await router.navigateByUrl('/login');
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

      const result = firstValueFrom(http.get('/api/accounts/ACC0011'));
      httpController
        .expectOne('/api/accounts/ACC0011')
        .flush(null, { status: 401, statusText: 'Unauthorized' });

      await expect(result).rejects.toMatchObject({ status: 401 });
      expect(navigate).not.toHaveBeenCalled();
    });
  });

  it.each([
    ['a 401 from /auth/login (wrong password)', '/auth/login', 401],
    ['a 403 from /api/**', '/api/accounts/ACC0001', 403],
    ['a 500 from /api/**', '/api/orders', 500],
  ])('does not clear the session or redirect for %s', async (_label, url, status) => {
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const result = firstValueFrom(http.post(url, {}));
    httpController.expectOne(url).flush(null, { status, statusText: 'Error' });

    await expect(result).rejects.toMatchObject({ status });
    expect(refresh).not.toHaveBeenCalled();
    expect(clearSession).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
  });

  describe('with a separate API origin configured (CORS deployment)', () => {
    const apiOrigin = 'https://api.example.com';

    beforeEach(() => {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [
          provideRouter([
            { path: 'orders', component: Blank },
            { path: 'login', component: Blank },
          ]),
          provideHttpClient(withInterceptors([authInterceptor])),
          provideHttpClientTesting(),
          { provide: Auth, useValue: { getToken: () => token, clearSession, refresh } },
          { provide: API_BASE_URL, useValue: apiOrigin },
        ],
      });
      http = TestBed.inject(HttpClient);
      httpController = TestBed.inject(HttpTestingController);
      router = TestBed.inject(Router);
    });

    it.each([`${apiOrigin}/api/accounts/ACC0011`, `${apiOrigin}/auth/me`, '/api/orders'])(
      'adds the Bearer token to %s',
      (url) => {
        expect(authorizationFor(url)).toBe('Bearer access.jwt.token');
      },
    );

    it.each([
      ['login on the API origin', `${apiOrigin}/auth/login`, 'POST'],
      ['another origin', 'https://evil.example.com/api/accounts/ACC0011', 'GET'],
      ['a look-alike host', 'https://api.example.com.evil.example.com/api/accounts', 'GET'],
      ['the API host over plain http', 'http://api.example.com/api/accounts', 'GET'],
    ] as const)('leaves the header off for %s', (_label, url, method) => {
      expect(authorizationFor(url, method)).toBeNull();
    });

    it('redirects to /login on a 401 from the API origin', async () => {
      await router.navigateByUrl('/orders');
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

      const result = firstValueFrom(http.get(`${apiOrigin}/api/orders`));
      httpController
        .expectOne(`${apiOrigin}/api/orders`)
        .flush(null, { status: 401, statusText: 'Unauthorized' });

      await expect(result).rejects.toMatchObject({ status: 401 });
      expect(clearSession).toHaveBeenCalledTimes(1);
      expect(navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/orders', expired: 1 },
      });
    });
  });

  describe('refreshing an expired session on a 401 (real Auth service)', () => {
    let auth: Auth;

    beforeEach(() => {
      TestBed.resetTestingModule();
      sessionStorage.setItem(
        SESSION_KEY,
        JSON.stringify({ accessToken: jwt(-1), refreshToken: 'refresh-1', user: demo }),
      );
      TestBed.configureTestingModule({
        providers: [
          provideRouter([
            { path: 'orders', component: Blank },
            { path: 'login', component: Blank },
          ]),
          provideHttpClient(withInterceptors([authInterceptor])),
          provideHttpClientTesting(),
        ],
      });
      http = TestBed.inject(HttpClient);
      httpController = TestBed.inject(HttpTestingController);
      router = TestBed.inject(Router);
      auth = TestBed.inject(Auth);
    });

    afterEach(() => sessionStorage.clear());

    const unauthorized = (url: string) =>
      httpController
        .expectOne(url)
        .flush(
          { errorCode: 'AUTH-401', message: 'Invalid or expired token' },
          { status: 401, statusText: 'Unauthorized' },
        );

    const renew = (accessToken: string) => {
      const request = httpController.expectOne('/auth/refresh');
      expect(request.request.body).toEqual({ refresh_token: 'refresh-1' });
      request.flush({ accessToken, refreshToken: 'refresh-2', expiresIn: 900, mfaRequired: false });
    };

    const retried = (url: string) => {
      const request = httpController.expectOne(url);
      return { request, authorization: request.request.headers.get('Authorization') };
    };

    it('calls /auth/refresh once and retries the original request with the new token', async () => {
      const result = firstValueFrom(http.get('/api/orders/account/ACC0011'));
      unauthorized('/api/orders/account/ACC0011');
      const newToken = jwt(900);
      renew(newToken);

      const retry = retried('/api/orders/account/ACC0011');
      retry.request.flush([{ orderId: 'ORD-1' }]);

      expect(retry.authorization).toBe(`Bearer ${newToken}`);
      expect(await result).toEqual([{ orderId: 'ORD-1' }]);
      expect(auth.getToken()).toBe(newToken);
    });

    it('shares one refresh call between requests that fail at the same time', async () => {
      const urls = [
        '/api/accounts/ACC0011',
        '/api/orders/account/ACC0011',
        '/api/positions/ACC0011',
      ];
      const results = Promise.all(urls.map((url) => firstValueFrom(http.get(url))));
      urls.forEach(unauthorized);
      const newToken = jwt(900);
      renew(newToken);

      for (const url of urls) {
        const retry = retried(url);
        expect(retry.authorization).toBe(`Bearer ${newToken}`);
        retry.request.flush({ url });
      }

      expect(await results).toEqual(urls.map((url) => ({ url })));
    });

    it('retries without refreshing again when another request already got a new token', async () => {
      const first = firstValueFrom(http.get('/api/accounts/ACC0011'));
      const late = firstValueFrom(http.get('/api/orders/account/ACC0011'));
      unauthorized('/api/accounts/ACC0011');
      const newToken = jwt(900);
      renew(newToken);
      retried('/api/accounts/ACC0011').request.flush({});
      await first;

      unauthorized('/api/orders/account/ACC0011');
      const retry = retried('/api/orders/account/ACC0011');
      retry.request.flush([]);

      httpController.expectNone('/auth/refresh');
      expect(retry.authorization).toBe(`Bearer ${newToken}`);
      expect(await late).toEqual([]);
    });

    it('logs out and goes to /login with the expired flag when the refresh fails', async () => {
      await router.navigateByUrl('/orders');
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

      const result = firstValueFrom(http.get('/api/orders/account/ACC0011'));
      unauthorized('/api/orders/account/ACC0011');
      httpController
        .expectOne('/auth/refresh')
        .flush(
          { errorCode: 'AUTH-401', message: 'Invalid refresh token' },
          { status: 401, statusText: 'Unauthorized' },
        );

      await expect(result).rejects.toMatchObject({ status: 401 });
      expect(auth.isAuthenticated()).toBe(false);
      expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
      expect(navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/orders', expired: 1 },
      });
    });

    it('retries only once: a second 401 logs out instead of refreshing again', async () => {
      await router.navigateByUrl('/orders');
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

      const result = firstValueFrom(http.get('/api/accounts/ACC0011'));
      unauthorized('/api/accounts/ACC0011');
      renew(jwt(900));
      unauthorized('/api/accounts/ACC0011');

      await expect(result).rejects.toMatchObject({ status: 401 });
      httpController.expectNone('/auth/refresh');
      expect(auth.isAuthenticated()).toBe(false);
      expect(navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/orders', expired: 1 },
      });
    });

    it('does not try to refresh when /auth/login says the password is wrong', async () => {
      const result = firstValueFrom(http.post('/auth/login', {}));
      unauthorized('/auth/login');

      await expect(result).rejects.toMatchObject({ status: 401 });
      httpController.expectNone('/auth/refresh');
    });
  });
});
