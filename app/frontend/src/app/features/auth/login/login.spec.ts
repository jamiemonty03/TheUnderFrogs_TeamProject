import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { HttpErrorResponse } from '@angular/common/http';
import { Login } from './login';
import { LOGIN_ERRORS, SESSION_EXPIRED_NOTICE } from './login-error';
import { Auth, CurrentUser } from '../../../services/auth';
import { Instruments } from '../../../services/instruments';

const demo: CurrentUser = { userId: 11, username: 'demo', roles: ['TRADER'], accountId: 'ACC0011' };

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let page: HTMLElement;
  let login: ReturnType<
    typeof vi.fn<(username: string, password: string) => Observable<CurrentUser>>
  >;
  let navigateByUrl: ReturnType<typeof vi.spyOn>;

  async function setUp(returnUrl: string | null = null, params: Record<string, string> = {}) {
    login = vi.fn(() => of(demo));
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        { provide: Auth, useValue: { login } },
        { provide: Instruments, useValue: { getLatestPrices: () => of(new Map()) } },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({ ...(returnUrl ? { returnUrl } : {}), ...params }),
            },
          },
        },
      ],
    }).compileComponents();
    navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(Login);
    page = fixture.nativeElement;
    fixture.detectChanges();
  }

  const input = (name: string) =>
    page.querySelector<HTMLInputElement>(`input[formcontrolname="${name}"]`)!;
  const button = () => page.querySelector<HTMLButtonElement>('button[type="submit"]')!;
  const alert = () => page.querySelector<HTMLElement>('[role="alert"]');
  const errors = () =>
    Array.from(page.querySelectorAll('mat-error')).map((e) => e.textContent?.trim());
  const type = (name: string, value: string) => {
    const field = input(name);
    field.value = value;
    field.dispatchEvent(new Event('input'));
    field.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
  };
  const submit = async () => {
    button().click();
    fixture.detectChanges();
    await fixture.whenStable();
  };
  const fillAndSubmit = async () => {
    type('username', 'demo');
    type('password', 'Demo123!');
    await submit();
  };

  describe('validation', () => {
    beforeEach(() => setUp());

    it('labels each field above its box, linked to the input', () => {
      expect(page.querySelector('label[for="login-username"]')?.textContent?.trim()).toBe(
        'Username',
      );
      expect(page.querySelector('label[for="login-password"]')?.textContent?.trim()).toBe(
        'Password',
      );
      expect(input('username').id).toBe('login-username');
      expect(input('password').id).toBe('login-password');
    });

    it('uses the shared auth layout with the "Welcome back" heading and the Log in tab active', () => {
      expect(page.querySelector('app-auth-layout h1')?.textContent?.trim()).toBe('Welcome back');
      expect(page.querySelector('.auth__subtitle')?.textContent?.trim()).toBe(
        'Log in to your Toadstock account.',
      );
      expect(page.querySelector('.auth__tab--active')?.textContent?.trim()).toBe('Log in');
      expect(page.querySelector('app-auth-layout form')).not.toBeNull();
    });

    it('has a username field and a masked password field', () => {
      expect(input('username').type).toBe('text');
      expect(input('password').type).toBe('password');
      expect(input('username').autocomplete).toBe('username');
      expect(input('password').autocomplete).toBe('current-password');
    });

    it('shows no errors before the form is submitted, even after leaving an empty field', () => {
      type('username', '');

      expect(errors()).toEqual([]);
    });

    it('shows both field errors when submitted empty, and does not try to log in', async () => {
      await submit();

      expect(errors()).toEqual(['Username is required.', 'Password is required.']);
      expect(login).not.toHaveBeenCalled();
    });

    it('shows only the missing field error', async () => {
      type('username', 'demo');

      await submit();

      expect(errors()).toEqual(['Password is required.']);
      expect(login).not.toHaveBeenCalled();
    });
  });

  describe('submitting', () => {
    it('logs in with the entered username and password', async () => {
      await setUp();

      await fillAndSubmit();

      expect(errors()).toEqual([]);
      expect(login).toHaveBeenCalledWith('demo', 'Demo123!');
    });

    it('disables the button and shows "Logging in…" while waiting, and ignores a second click', async () => {
      await setUp();
      const pending = new Subject<CurrentUser>();
      login.mockReturnValue(pending);

      await fillAndSubmit();
      await submit();

      expect(button().disabled).toBe(true);
      expect(button().textContent?.trim()).toBe('Logging in…');
      expect(login).toHaveBeenCalledTimes(1);

      pending.next(demo);
      pending.complete();
      fixture.detectChanges();
      expect(button().disabled).toBe(false);
      expect(button().textContent).toContain('Log in and hop in');
    });

    it('re-enables the button when login fails, and stays on the page', async () => {
      await setUp();
      login.mockReturnValue(throwError(() => new Error('failed')));

      await fillAndSubmit();

      expect(button().disabled).toBe(false);
      expect(navigateByUrl).not.toHaveBeenCalled();
    });
  });

  describe('redirect after a successful login', () => {
    it('goes to the returnUrl from the 401 redirect', async () => {
      await setUp('/orders?status=NEW');

      await fillAndSubmit();

      expect(navigateByUrl).toHaveBeenCalledWith('/orders?status=NEW');
    });

    it('goes to /dashboard when there is no returnUrl', async () => {
      await setUp();

      await fillAndSubmit();

      expect(navigateByUrl).toHaveBeenCalledWith('/dashboard');
    });

    it('goes to /dashboard instead of an unsafe returnUrl', async () => {
      await setUp('https://evil.example.com');

      await fillAndSubmit();

      expect(navigateByUrl).toHaveBeenCalledWith('/dashboard');
    });
  });

  describe('error messages', () => {
    const httpError = (status: number, errorCode?: string) =>
      new HttpErrorResponse({
        status,
        error: errorCode ? { errorCode, message: 'from server' } : null,
      });

    it.each([
      [
        'wrong username or password (AUTH-401)',
        httpError(401, 'AUTH-401'),
        LOGIN_ERRORS.invalidCredentials,
      ],
      ['too many attempts (AUTH-429)', httpError(429, 'AUTH-429'), LOGIN_ERRORS.tooManyAttempts],
      ['the sign-in service cannot be reached', httpError(0), LOGIN_ERRORS.unreachable],
    ])('shows the message for %s in an alert', async (_label, error, message) => {
      await setUp();
      login.mockReturnValue(throwError(() => error));

      await fillAndSubmit();

      expect(alert()?.textContent?.trim()).toBe(message);
      expect(navigateByUrl).not.toHaveBeenCalled();
    });

    it('shows no alert before anything has gone wrong', async () => {
      await setUp();

      expect(alert()).toBeNull();
    });

    it('clears the previous error when the user tries again', async () => {
      await setUp();
      login.mockReturnValue(throwError(() => httpError(401, 'AUTH-401')));
      await fillAndSubmit();
      expect(alert()).not.toBeNull();

      const pending = new Subject<CurrentUser>();
      login.mockReturnValue(pending);
      await submit();

      expect(alert()).toBeNull();
    });
  });

  describe('logging', () => {
    it('logs nothing, so never the password, when sign-in fails or succeeds', async () => {
      const spies = (['log', 'info', 'warn', 'error', 'debug'] as const).map((method) =>
        vi.spyOn(console, method).mockImplementation(() => undefined),
      );
      await setUp();
      login.mockReturnValueOnce(
        throwError(() => new HttpErrorResponse({ status: 401, error: { errorCode: 'AUTH-401' } })),
      );

      await fillAndSubmit();
      await submit();

      expect(JSON.stringify(spies.flatMap((spy) => spy.mock.calls))).not.toContain('Demo123!');
      spies.forEach((spy) => expect(spy).not.toHaveBeenCalled());
      vi.restoreAllMocks();
    });
  });

  describe('session expired notice', () => {
    const notice = () => page.querySelector<HTMLElement>('[data-testid="login-expired"]');

    it('explains why the user is here when the session expired', async () => {
      await setUp('/orders', { expired: '1' });

      expect(notice()?.textContent?.trim()).toBe(SESSION_EXPIRED_NOTICE);
      expect(notice()?.getAttribute('role')).toBe('status');
      expect(alert()).toBeNull();
    });

    it.each([
      ['no flag', {}],
      ['a different value', { expired: 'yes' }],
    ])('is not shown with %s', async (_label, params: Record<string, string>) => {
      await setUp(null, params);

      expect(notice()).toBeNull();
    });

    it('goes away once the user tries to log in again', async () => {
      await setUp('/orders', { expired: '1' });
      login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));

      await fillAndSubmit();

      expect(notice()).toBeNull();
      expect(alert()?.textContent?.trim()).toBe(LOGIN_ERRORS.invalidCredentials);
    });

    it('still returns to the page the user was on after logging in', async () => {
      await setUp('/orders', { expired: '1' });

      await fillAndSubmit();

      expect(navigateByUrl).toHaveBeenCalledWith('/orders');
    });
  });

  describe('showing the password', () => {
    const toggle = () =>
      page.querySelector<HTMLButtonElement>('[data-testid="login-toggle-password"]')!;
    const clickToggle = () => {
      toggle().click();
      fixture.detectChanges();
    };

    beforeEach(() => setUp());

    it('starts hidden, with a button that says it will show the password', () => {
      expect(input('password').type).toBe('password');
      expect(toggle().getAttribute('aria-label')).toBe('Show password');
      expect(toggle().getAttribute('aria-pressed')).toBe('false');
    });

    it('shows the password, then hides it again', () => {
      clickToggle();

      expect(input('password').type).toBe('text');
      expect(toggle().getAttribute('aria-label')).toBe('Hide password');
      expect(toggle().getAttribute('aria-pressed')).toBe('true');

      clickToggle();

      expect(input('password').type).toBe('password');
      expect(toggle().getAttribute('aria-label')).toBe('Show password');
    });

    it('keeps what was typed and does not submit the form', () => {
      type('password', 'Demo123!');

      clickToggle();

      expect(input('password').value).toBe('Demo123!');
      expect(toggle().type).toBe('button');
      expect(login).not.toHaveBeenCalled();
    });

    it('is still there and still works after a wrong password', async () => {
      login.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
      await fillAndSubmit();
      expect(alert()?.textContent?.trim()).toBe(LOGIN_ERRORS.invalidCredentials);

      clickToggle();

      expect(toggle()).not.toBeNull();
      expect(input('password').type).toBe('text');
      expect(input('password').value).toBe('Demo123!');
    });
  });
});
