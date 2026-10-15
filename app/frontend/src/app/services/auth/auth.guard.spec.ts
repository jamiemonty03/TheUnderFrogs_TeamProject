import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { vi } from 'vitest';
import { Auth, SESSION_KEY } from './auth';
import { authGuard } from './auth.guard';
import { CurrentUser } from '../interfaces/auth.interfaces';

@Component({ template: '' })
class Blank {}

function jwt(expiresInSeconds: number): string {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'RS256' })}.${encode({ sub: '11', exp: Math.floor(Date.now() / 1000) + expiresInSeconds })}.signature`;
}

const demo: CurrentUser = { userId: 11, username: 'demo', roles: ['TRADER'], accountId: 'ACC0011' };

function saveSession(accessToken: string, refreshToken: string | null = 'refresh-1') {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify({ accessToken, refreshToken, user: demo }));
}

describe('authGuard', () => {
  let harness: RouterTestingHarness;
  let router: Router;
  let http: HttpTestingController;

  async function setUp() {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'login', component: Blank },
          { path: 'dashboard', component: Blank, canActivate: [authGuard] },
          { path: 'orders', component: Blank, canActivate: [authGuard] },
        ]),
      ],
    });
    harness = await RouterTestingHarness.create();
    router = TestBed.inject(Router);
    http = TestBed.inject(HttpTestingController);
  }

  const refreshRequest = () => vi.waitFor(() => http.expectOne('/auth/refresh'));

  beforeEach(() => sessionStorage.clear());

  afterEach(() => http.verify());

  describe('with a valid access token', () => {
    it('lets the user through without refreshing', async () => {
      saveSession(jwt(900));
      await setUp();

      await harness.navigateByUrl('/orders');

      expect(router.url).toBe('/orders');
    });
  });

  describe('when signed out', () => {
    it('sends the user to /login with the page they wanted as returnUrl', async () => {
      await setUp();

      await harness.navigateByUrl('/orders?status=NEW');

      expect(router.url).toBe('/login?returnUrl=%2Forders%3Fstatus%3DNEW');
    });

    it('sends a user opening the dashboard to plain /login', async () => {
      await setUp();

      await harness.navigateByUrl('/dashboard');

      expect(router.url).toBe('/login');
    });

    it('treats an expired token with no refresh token as signed out, without the expired flag', async () => {
      saveSession(jwt(-1), null);
      await setUp();

      await harness.navigateByUrl('/orders');

      expect(router.url).toBe('/login?returnUrl=%2Forders');
      expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
    });
  });

  describe('with an expired access token and a refresh token', () => {
    it('refreshes first, then opens the page', async () => {
      saveSession(jwt(-1));
      await setUp();
      const auth = TestBed.inject(Auth);
      const newToken = jwt(900);

      const navigation = harness.navigateByUrl('/orders');
      const request = await refreshRequest();
      request.flush({
        accessToken: newToken,
        refreshToken: 'refresh-2',
        expiresIn: 900,
        mfaRequired: false,
      });
      await navigation;

      expect(request.request.body).toEqual({ refresh_token: 'refresh-1' });
      expect(router.url).toBe('/orders');
      expect(auth.getToken()).toBe(newToken);
      expect(auth.currentUser()).toEqual(demo);
    });

    it('treats a malformed access token like an expired one and refreshes it', async () => {
      saveSession('not-a-jwt');
      await setUp();

      const navigation = harness.navigateByUrl('/orders');
      (await refreshRequest()).flush({
        accessToken: jwt(900),
        refreshToken: 'refresh-2',
        expiresIn: 900,
        mfaRequired: false,
      });
      await navigation;

      expect(router.url).toBe('/orders');
    });

    it('sends the user to /login with the expired flag when the refresh fails', async () => {
      saveSession(jwt(-1));
      await setUp();
      const auth = TestBed.inject(Auth);

      const navigation = harness.navigateByUrl('/orders');
      (await refreshRequest()).flush(
        { errorCode: 'AUTH-401', message: 'Invalid refresh token' },
        { status: 401, statusText: 'Unauthorized' },
      );
      await navigation;

      expect(router.url).toBe('/login?returnUrl=%2Forders&expired=1');
      expect(auth.currentUser()).toBeNull();
      expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
    });

    it('keeps the dashboard URL clean but still flags the expiry', async () => {
      saveSession(jwt(-1));
      await setUp();

      const navigation = harness.navigateByUrl('/dashboard');
      (await refreshRequest()).flush(null, { status: 401, statusText: 'Unauthorized' });
      await navigation;

      expect(router.url).toBe('/login?expired=1');
    });
  });
});
