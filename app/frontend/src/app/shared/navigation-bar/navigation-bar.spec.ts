import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { NavigationBar } from './navigation-bar';
import { WalletDialog } from '../../features/wallet/wallet-dialog/wallet-dialog';
import { Theme } from '../../services/theme';
import { ProfileDialog } from '../../features/profile/profile-dialog/profile-dialog';
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

  const searchBox = () => page.querySelector('[data-testid="nav-search"]') as HTMLInputElement;
  const type = async (text: string) => {
    searchBox().value = text;
    searchBox().dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
  };
  const pressEnter = () =>
    searchBox().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
  const loadInstruments = () => {
    searchBox().focus();
    http.expectOne('/api/stocks').flush([
      { symbol: 'AAPL', name: 'Apple Inc.', price: 346.19 },
      { symbol: 'AMZN', name: 'Amazon.com, Inc.', price: 260.32 },
    ]);
    http.expectOne('/api/etfs').flush([{ symbol: 'SPY', name: 'SPDR S&P 500', price: 773.5 }]);
    http.expectOne('/api/bonds').flush([]);
  };

  it('suggests matching tickers as you type', async () => {
    await setUp();
    loadInstruments();

    await type('a');

    const options = [...document.querySelectorAll('.cdk-overlay-container mat-option')].map((o) =>
      o.textContent?.replace(/\s+/g, ' ').trim(),
    );
    expect(options).toEqual(['AAPL - Apple Inc. (Stock)', 'AMZN - Amazon.com, Inc. (Stock)']);
  });

  it('picking a suggestion opens its instrument page', async () => {
    await setUp();
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    loadInstruments();
    await type('spdr');

    (document.querySelector('[data-testid="search-option-SPY"]') as HTMLElement).click();

    expect(navigate).toHaveBeenCalledWith(['/instruments', 'SPY']);
  });

  it('Enter opens the typed ticker, or the top suggestion', async () => {
    await setUp();
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    loadInstruments();

    await type(' aapl ');
    pressEnter();
    expect(navigate).toHaveBeenLastCalledWith(['/instruments', 'AAPL']);

    await type('amaz');
    pressEnter();
    expect(navigate).toHaveBeenLastCalledWith(['/instruments', 'AMZN']);
  });

  it('Enter on an empty search does nothing', async () => {
    await setUp();
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    pressEnter();

    expect(navigate).not.toHaveBeenCalled();
  });

  describe('wallet', () => {
    const walletItem = () =>
      document.querySelector<HTMLButtonElement>('.cdk-overlay-container [data-testid="wallet"]');

    it('has Edit profile, Wallet, the theme switch and Log out, in that order, in the account menu', async () => {
      await setUp();

      await openMenu();

      const items = Array.from(
        document.querySelectorAll('.cdk-overlay-container [mat-menu-item]'),
      ).map((item) => item.textContent?.trim());
      expect(items).toEqual(['Edit profile', 'Wallet', 'Light mode', 'Log out']);
    });

    it('opens the wallet dialog', async () => {
      await setUp();
      const open = vi.spyOn(TestBed.inject(MatDialog), 'open').mockReturnValue({} as never);

      await openMenu();
      walletItem()!.click();

      await vi.waitFor(() => expect(open).toHaveBeenCalledTimes(1));
      expect(open.mock.calls[0][0]).toBe(WalletDialog);
      expect(open.mock.calls[0][1]).toMatchObject({ width: '440px', maxWidth: '95vw' });
    });
  });

  describe('edit profile', () => {
    it('opens the edit profile dialog', async () => {
      await setUp();
      const open = vi.spyOn(TestBed.inject(MatDialog), 'open').mockReturnValue({} as never);

      await openMenu();
      document
        .querySelector<HTMLButtonElement>('.cdk-overlay-container [data-testid="edit-profile"]')!
        .click();

      await vi.waitFor(() => expect(open).toHaveBeenCalledTimes(1));
      expect(open.mock.calls[0][0]).toBe(ProfileDialog);
      expect(open.mock.calls[0][1]).toMatchObject({ width: '480px', maxWidth: '95vw' });
    });
  });

  describe('light and dark mode', () => {
    const themeItem = () =>
      document.querySelector<HTMLButtonElement>(
        '.cdk-overlay-container [data-testid="theme-toggle"]',
      );

    beforeEach(() => {
      localStorage.clear();
      delete document.documentElement.dataset['theme'];
    });

    afterEach(() => {
      localStorage.clear();
      delete document.documentElement.dataset['theme'];
    });

    it('shows the white wordmark in dark mode and the dark green one in light mode', async () => {
      await setUp();
      const logo = () =>
        page.querySelector<HTMLImageElement>('.nav-bar__brand-mark')!.getAttribute('src');
      expect(logo()).toBe('/brand/toadstock-logo-dark.svg');

      TestBed.inject(Theme).set('light');
      fixture.detectChanges();
      expect(logo()).toBe('/brand/toadstock-logo-light.svg');
    });

    it('offers to switch to light mode from the dark theme, and then back', async () => {
      await setUp();
      await openMenu();
      expect(themeItem()?.textContent?.trim()).toBe('Light mode');

      themeItem()!.click();
      fixture.detectChanges();

      expect(document.documentElement.dataset['theme']).toBe('light');
      expect(TestBed.inject(Theme).theme()).toBe('light');

      await openMenu();
      expect(themeItem()?.textContent?.trim()).toBe('Dark mode');
    });
  });

  describe('main links', () => {
    it('shows Dashboard, Orders and Portfolios, with no Markets link', async () => {
      await setUp();

      const links = Array.from(page.querySelectorAll('.nav-bar__links a')).map((link) =>
        link.textContent?.trim(),
      );
      expect(links).toEqual(['Dashboard', 'Orders', 'Portfolios']);
      expect(page.querySelector('[data-testid="nav-markets"]')).toBeNull();
    });
  });
});
