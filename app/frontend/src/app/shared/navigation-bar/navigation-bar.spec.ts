import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { NavigationBar } from './navigation-bar';
import { Auth, SESSION_KEY, authGuard, authInterceptor, guestGuard } from '../../services/auth';
import { CurrentUser } from '../../services/interfaces/auth.interfaces';

@Component({ template: '' })
class Blank {}

function jwt(expiresInSeconds: number): string {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=+$/, '');
  return `${encode({ alg: 'RS256' })}.${encode({ sub: '11', exp: Math.floor(Date.now() / 1000) + expiresInSeconds })}.signature`;
}

const demo: CurrentUser = { userId: 11, username: 'demo', roles: ['TRADER'], accountId: 'ACC0011' };

describe('NavigationBar', () => {
  let fixture: ComponentFixture<NavigationBar>;
  let page: HTMLElement;
  let http: HttpTestingController;
  let router: Router;

  async function setUp(user: CurrentUser = demo) {
    sessionStorage.setItem(
      SESSION_KEY,
      JSON.stringify({ accessToken: jwt(900), refreshToken: 'refresh-1', user }),
    );
    await TestBed.configureTestingModule({
      imports: [NavigationBar],
      providers: [
        provideRouter([
          { path: 'login', component: Blank, canActivate: [guestGuard] },
          { path: 'dashboard', component: Blank, canActivate: [authGuard] },
        ]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    await router.navigateByUrl('/dashboard');
    fixture = TestBed.createComponent(NavigationBar);
    page = fixture.nativeElement;
    fixture.detectChanges();
  }

  const avatar = () => page.querySelector<HTMLButtonElement>('.nav-bar__avatar')!;
  const openMenu = async () => {
    avatar().click();
    fixture.detectChanges();
    await fixture.whenStable();
  };
  const menuText = (selector: string) =>
    document.querySelector(`.cdk-overlay-container ${selector}`)?.textContent?.trim();
  const logOutItem = () =>
    document.querySelector<HTMLButtonElement>('.cdk-overlay-container .nav-bar__logout');
  const logOut = async (
    respond: (request: ReturnType<HttpTestingController['expectOne']>) => void,
  ) => {
    await openMenu();
    logOutItem()!.click();
    respond(http.expectOne('/auth/logout'));
    await fixture.whenStable();
  };

  beforeEach(() => sessionStorage.clear());

  afterEach(() => {
    http.verify();
    document.querySelector('.cdk-overlay-container')?.replaceChildren();
  });

  describe('signed-in user details', () => {
    it.each([
      ['demo', 'D'],
      ['kermit.frog', 'K'],
      ['miss_piggy', 'M'],
      ['k', 'K'],
    ])('shows the first initial of %s as %s', async (username, initial) => {
      await setUp({ ...demo, username });

      expect(avatar().textContent?.trim()).toBe(initial);
    });

    it('no longer shows the hard-coded KF placeholder', async () => {
      await setUp();

      expect(avatar().textContent?.trim()).not.toBe('KF');
    });

    it('repeats the initial in the larger avatar inside the menu', async () => {
      await setUp();

      await openMenu();

      expect(menuText('.nav-bar__account-avatar')).toBe('D');
    });

    it('names the avatar button for screen readers', async () => {
      await setUp();

      expect(avatar().getAttribute('aria-label')).toBe('Account menu for demo');
    });

    it('shows the username and role in the account menu', async () => {
      await setUp();

      await openMenu();

      expect(menuText('.nav-bar__account-name')).toBe('demo');
      expect(menuText('.nav-bar__account-role')).toBe('Trader');
      expect(logOutItem()?.textContent?.trim()).toBe('Log out');
    });

    it('shows Admin as the role when the user has the ADMIN role', async () => {
      await setUp({ ...demo, roles: ['TRADER', 'ADMIN'] });

      await openMenu();

      expect(menuText('.nav-bar__account-role')).toBe('Admin');
    });
  });

  describe('log out', () => {
    it('revokes the refresh token on the server', async () => {
      await setUp();

      await logOut((request) => {
        expect(request.request.method).toBe('POST');
        expect(request.request.body).toEqual({ refresh_token: 'refresh-1' });
        request.flush(null, { status: 204, statusText: 'No Content' });
      });
    });

    it('clears the session and goes to the login page', async () => {
      await setUp();
      const auth = TestBed.inject(Auth);

      await logOut((request) => request.flush(null, { status: 204, statusText: 'No Content' }));

      expect(router.url).toBe('/login');
      expect(auth.isAuthenticated()).toBe(false);
      expect(auth.currentUser()).toBeNull();
      expect(sessionStorage.getItem(SESSION_KEY)).toBeNull();
    });

    it('replaces the history entry so Back cannot return to the page', async () => {
      await setUp();
      const navigate = vi.spyOn(router, 'navigate');

      await logOut((request) => request.flush(null, { status: 204, statusText: 'No Content' }));

      expect(navigate).toHaveBeenCalledWith(['/login'], { replaceUrl: true });
    });

    it('blocks a protected page afterwards, as pressing Back would', async () => {
      await setUp();
      await logOut((request) => request.flush(null, { status: 204, statusText: 'No Content' }));

      await router.navigateByUrl('/dashboard');

      expect(router.url).toBe('/login');
    });

    it('still logs out locally when the server call fails', async () => {
      await setUp();
      const auth = TestBed.inject(Auth);

      await logOut((request) => request.flush(null, { status: 503, statusText: 'Unavailable' }));

      expect(router.url).toBe('/login');
      expect(auth.isAuthenticated()).toBe(false);
    });
  });
});
