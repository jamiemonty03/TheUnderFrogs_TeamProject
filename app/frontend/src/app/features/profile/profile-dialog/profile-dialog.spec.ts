import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Auth } from '../../../services/auth';
import { Profile, ProfileChanges, UserProfile } from '../../../services/profile';
import { PROFILE_ERRORS } from '../profile-error';
import { DETAILS_SAVED, PASSWORD_CHANGED, ProfileDialog } from './profile-dialog';

const demo: UserProfile = { username: 'demo', email: 'demo@toadstock.io', fullName: 'Demo Trader' };

describe('ProfileDialog', () => {
  let fixture: ComponentFixture<ProfileDialog>;
  let page: HTMLElement;
  let getProfile: ReturnType<typeof vi.fn<() => Observable<UserProfile>>>;
  let updateProfile: ReturnType<typeof vi.fn<(changes: ProfileChanges) => Observable<UserProfile>>>;
  let changePassword: ReturnType<typeof vi.fn<(current: string, next: string) => Observable<void>>>;
  let close: ReturnType<typeof vi.fn>;
  let snackBarOpen: ReturnType<typeof vi.fn>;

  async function setUp(profile$: Observable<UserProfile> = of(demo)) {
    getProfile = vi.fn(() => profile$);
    updateProfile = vi.fn((changes: ProfileChanges) => of({ ...demo, ...changes }));
    changePassword = vi.fn(() => of(undefined));
    close = vi.fn();
    snackBarOpen = vi.fn();
    await TestBed.configureTestingModule({
      imports: [ProfileDialog],
      providers: [
        { provide: Profile, useValue: { getProfile, updateProfile } },
        { provide: Auth, useValue: { changePassword } },
        { provide: MatDialogRef, useValue: { close } },
        { provide: MatSnackBar, useValue: { open: snackBarOpen } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ProfileDialog);
    page = fixture.nativeElement;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  const byTestId = <T extends HTMLElement = HTMLElement>(id: string) =>
    page.querySelector<T>(`[data-testid="${id}"]`);
  const input = (id: string) => byTestId<HTMLInputElement>(id)!;
  const errors = () =>
    Array.from(page.querySelectorAll('mat-error')).map((e) => e.textContent?.trim());
  const type = (id: string, value: string) => {
    input(id).value = value;
    input(id).dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };
  const click = async (id: string) => {
    byTestId<HTMLButtonElement>(id)!.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  };
  const openPasswordTab = async () => {
    page.querySelectorAll<HTMLElement>('[role="tab"]')[1].click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  };
  const httpError = (status: number, message?: string) =>
    new HttpErrorResponse({ status, error: message ? { errorCode: 'X', message } : null });

  describe('details', () => {
    it('shows "Loading your profile…" until the profile arrives', async () => {
      await setUp(new Subject<UserProfile>());

      expect(byTestId('profile-loading')?.textContent).toContain('Loading your profile…');
      expect(byTestId('profile-loading')?.getAttribute('role')).toBe('status');
    });

    it('fills in the form, and shows the username as something that cannot be changed', async () => {
      await setUp();

      expect(byTestId('profile-username')?.textContent?.trim()).toBe('demo');
      expect(page.textContent).toContain("Your username can't be changed.");
      expect(input('profile-full-name').value).toBe('Demo Trader');
      expect(input('profile-email').value).toBe('demo@toadstock.io');
    });

    it('explains when the profile cannot be loaded', async () => {
      await setUp(throwError(() => httpError(0)));

      expect(byTestId('profile-load-error')?.textContent?.trim()).toBe(PROFILE_ERRORS.loadFailed);
    });

    it('sends only the fields that changed, then closes and confirms', async () => {
      await setUp();
      type('profile-full-name', 'Kermit T. Frog');

      await click('profile-save-details');

      expect(updateProfile).toHaveBeenCalledWith({ fullName: 'Kermit T. Frog' });
      expect(close).toHaveBeenCalled();
      expect(snackBarOpen).toHaveBeenCalledWith(DETAILS_SAVED, 'OK', { duration: 6000 });
    });

    it('trims spaces before sending', async () => {
      await setUp();
      type('profile-email', '  kermit@pond.io  ');

      await click('profile-save-details');

      expect(updateProfile).toHaveBeenCalledWith({ email: 'kermit@pond.io' });
    });

    it('closes and confirms without calling the server when nothing changed', async () => {
      await setUp();

      await click('profile-save-details');

      expect(updateProfile).not.toHaveBeenCalled();
      expect(close).toHaveBeenCalled();
      expect(snackBarOpen).toHaveBeenCalledWith(DETAILS_SAVED, 'OK', { duration: 6000 });
    });

    it.each([
      ['profile-full-name', '   ', 'Full name is required.'],
      ['profile-full-name', 'x'.repeat(256), 'Full name must be 255 characters or fewer.'],
      ['profile-email', '', 'Email is required.'],
      ['profile-email', 'kermit@pond', 'Enter a valid email address.'],
    ])('rejects %s = "%s" with "%s" and does not save', async (field, value, message) => {
      await setUp();
      type(field, value);

      await click('profile-save-details');

      expect(errors()).toEqual([message]);
      expect(updateProfile).not.toHaveBeenCalled();
    });

    it('shows a taken email under the email field and stays open', async () => {
      await setUp();
      updateProfile.mockReturnValue(
        throwError(() => httpError(409, 'That email is already used by another account')),
      );
      type('profile-email', 'taken@pond.io');

      await click('profile-save-details');

      expect(errors()).toEqual(['That email is already used by another account.']);
      expect(close).not.toHaveBeenCalled();
      expect(snackBarOpen).not.toHaveBeenCalled();
    });

    it('shows "Saving…" and ignores a second click while waiting', async () => {
      await setUp();
      updateProfile.mockReturnValue(new Subject<UserProfile>());
      type('profile-full-name', 'Kermit');

      await click('profile-save-details');
      await click('profile-save-details');

      expect(byTestId<HTMLButtonElement>('profile-save-details')!.disabled).toBe(true);
      expect(byTestId('profile-save-details')?.textContent?.trim()).toBe('Saving…');
      expect(updateProfile).toHaveBeenCalledTimes(1);
    });
  });

  describe('password', () => {
    const fill = (current: string, next: string, confirm = next) => {
      type('profile-current-password', current);
      type('profile-new-password', next);
      type('profile-confirm-password', confirm);
    };

    beforeEach(async () => {
      await setUp();
      await openPasswordTab();
    });

    it('starts with all three password boxes hidden', () => {
      expect(input('profile-current-password').type).toBe('password');
      expect(input('profile-new-password').type).toBe('password');
      expect(input('profile-confirm-password').type).toBe('password');
    });

    it('can show the current and new passwords', async () => {
      const [showCurrent, showNew] = Array.from(
        page.querySelectorAll<HTMLButtonElement>('.profile__reveal'),
      );

      showCurrent.click();
      showNew.click();
      fixture.detectChanges();

      expect(input('profile-current-password').type).toBe('text');
      expect(input('profile-new-password').type).toBe('text');
      expect(input('profile-confirm-password').type).toBe('text');
    });

    it('changes the password, closes the dialog and says other devices were signed out', async () => {
      fill('Demo123!', 'lily-pad-leap-2026');

      await click('profile-change-password');

      expect(changePassword).toHaveBeenCalledWith('Demo123!', 'lily-pad-leap-2026');
      expect(close).toHaveBeenCalledTimes(1);
      expect(snackBarOpen).toHaveBeenCalledWith(PASSWORD_CHANGED, 'OK', { duration: 6000 });
    });

    it.each([
      ['short', 'Password must be at least 12 characters.'],
      [' '.repeat(12), "Password can't be only spaces."],
      ['x'.repeat(129), 'Password must be 128 characters or fewer.'],
    ])('rejects a new password of "%s"', async (next, message) => {
      fill('Demo123!', next);

      await click('profile-change-password');

      expect(errors()).toEqual([message]);
      expect(changePassword).not.toHaveBeenCalled();
    });

    it('rejects new passwords that do not match', async () => {
      fill('Demo123!', 'lily-pad-leap-2026', 'lily-pad-leap-2027');

      await click('profile-change-password');

      expect(byTestId('profile-mismatch')?.textContent?.trim()).toBe(
        "The new passwords don't match.",
      );
      expect(changePassword).not.toHaveBeenCalled();
    });

    it('shows a wrong current password under its field and stays open', async () => {
      changePassword.mockReturnValue(
        throwError(() => httpError(403, 'Your current password is incorrect')),
      );
      fill('wrong-one', 'lily-pad-leap-2026');

      await click('profile-change-password');

      expect(errors()).toEqual(['Your current password is incorrect.']);
      expect(close).not.toHaveBeenCalled();
      expect(snackBarOpen).not.toHaveBeenCalled();
    });

    it.each([
      [
        'a password that did not change (422)',
        httpError(422, 'Choose a new password that is different from your current one'),
        'Choose a new password that is different from your current one.',
      ],
      ['too many attempts (429)', httpError(429), PROFILE_ERRORS.tooManyAttempts],
      ['the service being unreachable', httpError(0), PROFILE_ERRORS.unreachable],
    ])('explains %s', async (_label, error, message) => {
      changePassword.mockReturnValue(throwError(() => error));
      fill('Demo123!', 'lily-pad-leap-2026');

      await click('profile-change-password');

      expect(byTestId('profile-password-error')?.textContent?.trim()).toBe(message);
      expect(byTestId<HTMLButtonElement>('profile-change-password')!.disabled).toBe(false);
    });
  });

  it('closes when the close button is clicked', async () => {
    await setUp();

    byTestId<HTMLButtonElement>('profile-close')!.click();

    expect(close).toHaveBeenCalled();
  });
});
