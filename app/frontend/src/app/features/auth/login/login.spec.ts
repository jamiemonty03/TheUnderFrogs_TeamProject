import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let page: HTMLElement;

  const input = (name: string) =>
    page.querySelector<HTMLInputElement>(`input[formcontrolname="${name}"]`)!;
  const errors = () =>
    Array.from(page.querySelectorAll('mat-error')).map((e) => e.textContent?.trim());
  const submit = async () => {
    page.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
    await fixture.whenStable();
  };
  const type = (name: string, value: string) => {
    const field = input(name);
    field.value = value;
    field.dispatchEvent(new Event('input'));
    field.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Login] }).compileComponents();
    fixture = TestBed.createComponent(Login);
    page = fixture.nativeElement;
    fixture.detectChanges();
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

  it('shows both field errors when submitted empty', async () => {
    await submit();

    expect(errors()).toEqual(['Username is required.', 'Password is required.']);
  });

  it('shows only the missing field error', async () => {
    type('username', 'demo');

    await submit();

    expect(errors()).toEqual(['Password is required.']);
  });

  it('treats the form as valid once both fields are filled', async () => {
    type('username', 'demo');
    type('password', 'Demo123!');

    await submit();

    expect(errors()).toEqual([]);
    expect(fixture.componentInstance.form.valid).toBe(true);
  });
});
