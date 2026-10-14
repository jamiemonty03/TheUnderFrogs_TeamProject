import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
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

function saveSession(accessToken: string) {
  sessionStorage.setItem(
    SESSION_KEY,
    JSON.stringify({ accessToken, refreshToken: 'refresh-1', user: demo }),
  );
}

describe('authGuard', () => {
  let harness: RouterTestingHarness;
  let router: Router;

  async function setUp() {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideRouter([
          { path: 'login', component: Blank },
          { path: 'dashboard', component: Blank, canActivate: [authGuard] },
          { path: 'orders', component: Blank, canActivate: [authGuard] },
        ]),
      ],
    });
    harness = await RouterTestingHarness.create();
    router = TestBed.inject(Router);
  }

  beforeEach(() => sessionStorage.clear());

  it('lets a signed-in user with a valid token through', async () => {
    saveSession(jwt(900));
    await setUp();

    await harness.navigateByUrl('/orders');

    expect(router.url).toBe('/orders');
  });

  it('sends a signed-out user to /login with the page they wanted as returnUrl', async () => {
    await setUp();

    await harness.navigateByUrl('/orders?status=NEW');

    expect(router.url).toBe('/login?returnUrl=%2Forders%3Fstatus%3DNEW');
  });

  it('sends a signed-out user opening the dashboard to plain /login', async () => {
    await setUp();

    await harness.navigateByUrl('/dashboard');

    expect(router.url).toBe('/login');
  });

  it('treats a token past its exp time as signed out', async () => {
    saveSession(jwt(-1));
    await setUp();

    await harness.navigateByUrl('/orders');

    expect(router.url).toBe('/login?returnUrl=%2Forders');
  });

  it('clears the expired session so no old user details are left behind', async () => {
    saveSession(jwt(-1));
    await setUp();
    const auth = TestBed.inject(Auth);
    expect(auth.currentUser()).toEqual(demo);

    await harness.navigateByUrl('/orders');

    expect(auth.currentUser()).toBeNull();
    expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
  });

  it('treats a malformed token as signed out', async () => {
    saveSession('not-a-jwt');
    await setUp();

    await harness.navigateByUrl('/orders');

    expect(router.url).toBe('/login?returnUrl=%2Forders');
  });
});
