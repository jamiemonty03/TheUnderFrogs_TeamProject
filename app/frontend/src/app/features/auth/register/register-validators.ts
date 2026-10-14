import { AbstractControl, ValidationErrors } from '@angular/forms';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const USERNAME_PATTERN = /^[A-Za-z0-9._-]+$/;

export function notBlank(control: AbstractControl<string>): ValidationErrors | null {
  const value = control.value ?? '';
  return value.length > 0 && value.trim().length === 0 ? { blank: true } : null;
}

export function strictEmail(control: AbstractControl<string>): ValidationErrors | null {
  const value = control.value ?? '';
  return value.length > 0 && !EMAIL_PATTERN.test(value) ? { email: true } : null;
}
