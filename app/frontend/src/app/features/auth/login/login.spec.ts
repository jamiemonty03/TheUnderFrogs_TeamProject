import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { HttpErrorResponse } from '@angular/common/http';
import { Login } from './login';
import { LOGIN_ERRORS } from './login-error';
import { Auth, CurrentUser } from '../../../services/auth';

const demo: CurrentUser = { userId: 11, username: 'demo', roles: ['TRADER'], accountId: 'ACC0011' };

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let page: HTMLElement;
  let login: ReturnType<
    typeof vi.fn<(username: string, password: string) => Observable<CurrentUser>>
  >;
  let navigateByUrl: ReturnType<typeof vi.spyOn>;

  async function setUp(returnUrl: string | null = null) {
    login = vi.fn(() => of(demo));
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        { provide: Auth, useValue: { login } },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap(returnUrl ? { returnUrl } : {}) },
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

    it('disables the button and shows "Signing in…" while waiting, and ignores a second click', async () => {
      await setUp();
      const pending = new Subject<CurrentUser>();
      login.mockReturnValue(pending);

      await fillAndSubmit();
      await submit();

      expect(button().disabled).toBe(true);
      expect(button().textContent?.trim()).toBe('Signing in…');
      expect(login).toHaveBeenCalledTimes(1);

      pending.next(demo);
      pending.complete();
      fixture.detectChanges();
      expect(button().disabled).toBe(false);
      expect(button().textContent?.trim()).toBe('Sign in');
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
});
