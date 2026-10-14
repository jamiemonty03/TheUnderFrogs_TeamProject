import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { SESSION_KEY } from './auth';
import { guestGuard } from './guest.guard';

@Component({ template: '' })
class Blank {}

function jwt(expiresInSeconds: number): string {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'RS256' })}.${encode({ sub: '11', exp: Math.floor(Date.now() / 1000) + expiresInSeconds })}.signature`;
}

function saveSession(accessToken: string) {
  sessionStorage.setItem(
    SESSION_KEY,
    JSON.stringify({
      accessToken,
      refreshToken: 'refresh-1',
      user: { userId: 11, username: 'demo', roles: ['TRADER'], accountId: 'ACC0011' },
    }),
  );
}

describe('guestGuard', () => {
  let harness: RouterTestingHarness;
  let router: Router;

  async function setUp() {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideRouter([
          { path: 'login', component: Blank, canActivate: [guestGuard] },
          { path: 'register', component: Blank, canActivate: [guestGuard] },
          { path: 'dashboard', component: Blank },
        ]),
      ],
    });
    harness = await RouterTestingHarness.create();
    router = TestBed.inject(Router);
  }

  beforeEach(() => sessionStorage.clear());

  it.each(['/login', '/register'])('lets a signed-out user open %s', async (url) => {
    await setUp();

    await harness.navigateByUrl(url);

    expect(router.url).toBe(url);
  });

  it.each(['/login', '/register'])('sends a signed-in user from %s to /dashboard', async (url) => {
    saveSession(jwt(900));
    await setUp();

    await harness.navigateByUrl(url);

    expect(router.url).toBe('/dashboard');
  });

  it('ignores a returnUrl when a signed-in user lands on /login', async () => {
    saveSession(jwt(900));
    await setUp();

    await harness.navigateByUrl('/login?returnUrl=%2Forders');

    expect(router.url).toBe('/dashboard');
  });

  it('lets a user with an expired token open /login to sign in again', async () => {
    saveSession(jwt(-1));
    await setUp();

    await harness.navigateByUrl('/login');

    expect(router.url).toBe('/login');
  });
});
