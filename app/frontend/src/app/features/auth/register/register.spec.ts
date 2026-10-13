import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Register } from './register';

describe('Register', () => {
  let fixture: ComponentFixture<Register>;
  let page: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [provideRouter([])],
    }).compileComponents();
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
  const submit = async () => {
    page.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
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

  it('shows a required error for every field when submitted empty', async () => {
    await submit();

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
});
