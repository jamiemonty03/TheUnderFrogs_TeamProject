import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { EMPTY, Observable, Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Register } from './register';
import { REGISTER_ERRORS } from './register-error';
import { Auth, CurrentUser, RegisterDetails } from '../../../services/auth';
import { Instruments } from '../../../services/instruments';
import { LivePrices } from '../../../services/live-prices';

const kermit: CurrentUser = {
  userId: 42,
  username: 'kermit',
  roles: ['TRADER'],
  accountId: 'ACC0042',
};

describe('Register', () => {
  let fixture: ComponentFixture<Register>;
  let page: HTMLElement;
  let register: ReturnType<typeof vi.fn<(details: RegisterDetails) => Observable<CurrentUser>>>;
  let navigateByUrl: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    register = vi.fn(() => of(kermit));
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [
        provideRouter([]),
        { provide: Auth, useValue: { register } },
        { provide: Instruments, useValue: { getLatestPrices: () => of(new Map()) } },
        { provide: LivePrices, useValue: { watch: () => EMPTY } },
      ],
    }).compileComponents();
    navigateByUrl = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture = TestBed.createComponent(Register);
    page = fixture.nativeElement;
    fixture.detectChanges();
  });

  const input = (name: string) =>
    page.querySelector<HTMLInputElement>(`input[formcontrolname="${name}"]`)!;
  const errors = () =>
    Array.from(page.querySelectorAll('mat-error')).map((e) => e.textContent?.trim());
  const type = (name: string, value: string) => {
    const field = input(name);
    field.value = value;
    field.dispatchEvent(new Event('input'));
    field.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
  };
  const button = () => page.querySelector<HTMLButtonElement>('button[type="submit"]')!;
  const submit = async () => {
    button().click();
    fixture.detectChanges();
    await fixture.whenStable();
  };
  const fillValid = () => {
    type('fullName', 'Kermit T. Frog');
    type('username', 'kermit');
    type('email', 'kermit@pond.io');
    type('password', 'LilyPad-2026!');
  };

  it('uses the shared auth layout with "Join the pond" and the Create account tab active', () => {
    expect(page.querySelector('app-auth-layout h1')?.textContent?.trim()).toBe('Join the pond');
    expect(page.querySelector('.auth__subtitle')?.textContent?.trim()).toBe(
      'Start trading in under a minute.',
    );
    expect(page.querySelector('.auth__tab--active')?.textContent?.trim()).toBe('Create account');
  });

  it('labels each field above its box, linked to the input', () => {
    const labels: [string, string, string][] = [
      ['fullName', 'register-full-name', 'Full name'],
      ['username', 'register-username', 'Username'],
      ['email', 'register-email', 'Email'],
      ['password', 'register-password', 'Password'],
    ];
    for (const [name, id, text] of labels) {
      expect(input(name).id).toBe(id);
      expect(page.querySelector(`label[for="${id}"]`)?.textContent?.trim()).toBe(text);
    }
  });

  it('puts username and email side by side', () => {
    const row = page.querySelector('.register__row')!;

    expect(row.querySelector('#register-username')).not.toBeNull();
    expect(row.querySelector('#register-email')).not.toBeNull();
  });

  it('uses the right input types and autocomplete hints', () => {
    expect(input('fullName').autocomplete).toBe('name');
    expect(input('username').autocomplete).toBe('username');
    expect(input('email').type).toBe('email');
    expect(input('email').autocomplete).toBe('email');
    expect(input('password').type).toBe('password');
    expect(input('password').autocomplete).toBe('new-password');
    expect(input('password').placeholder).toBe('At least 12 characters');
  });

  it('shows the submit button and the cash account note', () => {
    expect(page.querySelector('button[type="submit"]')?.textContent?.trim()).toBe(
      'Create trading account',
    );
    expect(page.querySelector('.register__note')?.textContent?.trim()).toBe(
      'We open a cash account for you automatically.',
    );
  });

  it('shows no errors before the form is submitted', () => {
    type('username', 'k');

    expect(errors()).toEqual([]);
  });

  it('shows a required error for every field when submitted empty, and does not register', async () => {
    await submit();

    expect(register).not.toHaveBeenCalled();

    expect(errors()).toEqual([
      'Full name is required.',
      'Username is required.',
      'Email is required.',
      'Password is required.',
    ]);
  });

  it('shows no errors and is valid when every field is filled in correctly', async () => {
    fillValid();

    await submit();

    expect(errors()).toEqual([]);
    expect(fixture.componentInstance.form.valid).toBe(true);
  });

  describe('field rules', () => {
    it.each([
      ['fullName', '   ', 'Full name is required.'],
      ['fullName', 'a'.repeat(256), 'Full name must be 255 characters or fewer.'],
      ['username', 'ke', 'Username must be at least 3 characters.'],
      ['username', 'k'.repeat(51), 'Username must be 50 characters or fewer.'],
      ['username', 'kermit frog', 'Use only letters, numbers, dots, underscores and hyphens.'],
      ['username', 'kermit@pond', 'Use only letters, numbers, dots, underscores and hyphens.'],
      ['email', 'kermit', 'Enter a valid email address.'],
      ['email', 'kermit@pond', 'Enter a valid email address.'],
      ['password', 'short', 'Password must be at least 12 characters.'],
      ['password', ' '.repeat(12), "Password can't be only spaces."],
      ['password', 'p'.repeat(129), 'Password must be 128 characters or fewer.'],
    ])('%s "%s" shows "%s"', async (name, value, message) => {
      fillValid();
      type(name, value);

      await submit();

      expect(errors()).toEqual([message]);
    });

    it.each([
      ['username', 'kermit.the_frog-1'],
      ['username', 'kmt'],
      ['email', 'kermit.frog@pond.co.uk'],
      ['password', 'p'.repeat(128)],
      ['fullName', 'a'.repeat(255)],
    ])('accepts %s "%s"', async (name, value) => {
      fillValid();
      type(name, value);

      await submit();

      expect(errors()).toEqual([]);
    });
  });

  describe('submitting', () => {
    it('does not register while any field is invalid', async () => {
      fillValid();
      type('email', 'kermit@pond');

      await submit();

      expect(register).not.toHaveBeenCalled();
    });

    it('registers with the entered details, trimming the full name and email', async () => {
      fillValid();
      type('fullName', '  Kermit T. Frog  ');
      type('email', ' kermit@pond.io ');

      await submit();

      expect(register).toHaveBeenCalledWith({
        fullName: 'Kermit T. Frog',
        username: 'kermit',
        email: 'kermit@pond.io',
        password: 'LilyPad-2026!',
      });
    });

    it('goes to /dashboard once the account is created', async () => {
      fillValid();

      await submit();

      expect(navigateByUrl).toHaveBeenCalledWith('/dashboard');
    });

    it('disables the button and shows "Creating account…" while waiting, and ignores a second click', async () => {
      const pending = new Subject<CurrentUser>();
      register.mockReturnValue(pending);
      fillValid();

      await submit();
      await submit();

      expect(button().disabled).toBe(true);
      expect(button().textContent?.trim()).toBe('Creating account…');
      expect(register).toHaveBeenCalledTimes(1);

      pending.next(kermit);
      pending.complete();
      fixture.detectChanges();
      expect(button().disabled).toBe(false);
      expect(button().textContent?.trim()).toBe('Create trading account');
    });

    it('re-enables the button when registering fails, and stays on the page', async () => {
      register.mockReturnValue(throwError(() => new Error('failed')));
      fillValid();

      await submit();

      expect(button().disabled).toBe(false);
      expect(navigateByUrl).not.toHaveBeenCalled();
    });
  });

  describe('error messages', () => {
    const alert = () => page.querySelector<HTMLElement>('[role="alert"]');
    const httpError = (status: number, errorCode?: string) =>
      new HttpErrorResponse({
        status,
        error: errorCode ? { errorCode, message: 'from server' } : null,
      });

    it('shows no alert before anything goes wrong', async () => {
      fillValid();

      await submit();

      expect(alert()).toBeNull();
    });

    it.each([
      [
        'the username or email is taken (AUTH-409)',
        httpError(409, 'AUTH-409'),
        REGISTER_ERRORS.taken,
      ],
      ['too many attempts (AUTH-429)', httpError(429, 'AUTH-429'), REGISTER_ERRORS.tooManyAttempts],
      [
        'the trading account could not be opened (AUTH-503)',
        httpError(503, 'AUTH-503'),
        REGISTER_ERRORS.accountNotOpened,
      ],
      ['the service cannot be reached', httpError(0), REGISTER_ERRORS.unreachable],
    ])('shows the message for %s in an alert', async (_label, error, message) => {
      register.mockReturnValue(throwError(() => error));
      fillValid();

      await submit();

      expect(alert()?.textContent?.trim()).toBe(message);
      expect(navigateByUrl).not.toHaveBeenCalled();
    });

    it('keeps what the user typed so they can fix it and try again', async () => {
      register.mockReturnValue(throwError(() => httpError(409, 'AUTH-409')));
      fillValid();

      await submit();

      expect(input('username').value).toBe('kermit');
      expect(input('email').value).toBe('kermit@pond.io');
      expect(input('password').value).toBe('LilyPad-2026!');
    });

    it('clears the alert when trying again, and goes to /dashboard if it works', async () => {
      register.mockReturnValueOnce(throwError(() => httpError(409, 'AUTH-409')));
      fillValid();
      await submit();
      expect(alert()).not.toBeNull();

      type('username', 'kermit2');
      await submit();

      expect(alert()).toBeNull();
      expect(navigateByUrl).toHaveBeenCalledWith('/dashboard');
    });
  });
});
