import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { vi } from 'vitest';
import { Component } from '@angular/core';
import { authInterceptor } from './auth.interceptor';
import { Auth } from './auth';
import { API_BASE_URL } from './api-config';

@Component({ template: '' })
class Blank {}

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpController: HttpTestingController;
  let router: Router;
  let token: string | null;
  const clearSession = vi.fn();

  beforeEach(() => {
    token = 'access.jwt.token';
    clearSession.mockReset();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'orders', component: Blank },
          { path: 'login', component: Blank },
        ]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: Auth, useValue: { getToken: () => token, clearSession } },
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

  describe('when an API call returns 401', () => {
    it('clears the session and redirects to /login with the current URL as returnUrl', async () => {
      await router.navigateByUrl('/orders?status=NEW');
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

      const result = firstValueFrom(http.get('/api/orders/account/ACC0011'));
      httpController
        .expectOne('/api/orders/account/ACC0011')
        .flush(null, { status: 401, statusText: 'Unauthorized' });

      await expect(result).rejects.toMatchObject({ status: 401 });
      expect(clearSession).toHaveBeenCalledTimes(1);
      expect(navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/orders?status=NEW' },
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
          { provide: Auth, useValue: { getToken: () => token, clearSession } },
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
      expect(navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/orders' } });
    });
  });
});
