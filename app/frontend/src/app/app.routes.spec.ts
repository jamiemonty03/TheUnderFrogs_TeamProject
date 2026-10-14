import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
import { SESSION_KEY, authGuard, guestGuard } from './services/auth';

function jwt(expiresInSeconds: number): string {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'RS256' })}.${encode({ sub: '11', exp: Math.floor(Date.now() / 1000) + expiresInSeconds })}.signature`;
}

function signIn() {
  sessionStorage.setItem(
    SESSION_KEY,
    JSON.stringify({
      accessToken: jwt(900),
      refreshToken: 'refresh-1',
      user: { userId: 11, username: 'demo', roles: ['TRADER'], accountId: 'ACC0011' },
    }),
  );
}

const guardedGroup = routes.find((route) => route.canActivateChild?.includes(authGuard))!;

const protectedUrls = guardedGroup
  .children!.filter((route) => route.loadComponent)
  .map((route) => '/' + route.path!.replace(/:[^/]+/g, '1'));

describe('app routes', () => {
  let harness: RouterTestingHarness;
  let router: Router;

  async function setUp() {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
    harness = await RouterTestingHarness.create();
    router = TestBed.inject(Router);
  }

  async function open(url: string) {
    await harness.navigateByUrl(url);
    return router.url;
  }

  beforeEach(() => sessionStorage.clear());

  describe('config', () => {
    it('only lets /login and /register sit outside the guarded group', () => {
      const pagesOutside = routes
        .filter((route) => route !== guardedGroup && route.loadComponent)
        .map((route) => route.path);

      expect(pagesOutside).toEqual(['login', 'register']);
    });

    it('puts guestGuard on /login and /register', () => {
      for (const path of ['login', 'register']) {
        expect(routes.find((route) => route.path === path)?.canActivate).toContain(guestGuard);
      }
    });

    it('lazy-loads every page so the first download stays small', () => {
      const pages = [...routes, ...guardedGroup.children!].filter(
        (route) => !route.redirectTo && !route.children,
      );

      expect(pages.every((route) => route.loadComponent && !route.component)).toBe(true);
    });

    it('finds the pages to check', () => {
      expect(protectedUrls).toEqual(
        expect.arrayContaining(['/dashboard', '/orders', '/positions', '/instruments']),
      );
    });
  });

  describe('signed out', () => {
    beforeEach(() => setUp());

    it('opens the login page first when visiting the site', async () => {
      expect(await open('/')).toBe('/login');
    });

    it('sends an unknown address to the login page', async () => {
      expect(await open('/no-such-page')).toBe('/login');
    });

    it.each(protectedUrls.filter((url) => url !== '/dashboard'))(
      'guards %s and remembers it as the returnUrl',
      async (url) => {
        expect(await open(url)).toBe(`/login?returnUrl=${encodeURIComponent(url)}`);
      },
    );

    it('guards /dashboard and goes to plain /login', async () => {
      expect(await open('/dashboard')).toBe('/login');
    });

    it.each(['/login', '/register'])('can open %s', async (url) => {
      expect(await open(url)).toBe(url);
    });
  });

  describe('signed in', () => {
    beforeEach(async () => {
      signIn();
      await setUp();
    });

    it('opens the dashboard when visiting the site', async () => {
      expect(await open('/')).toBe('/dashboard');
    });

    it.each(protectedUrls)('can open %s', async (url) => {
      expect(await open(url)).toBe(url);
    });

    it.each(['/login', '/register'])('is sent from %s to the dashboard', async (url) => {
      expect(await open(url)).toBe('/dashboard');
    });
  });
});
