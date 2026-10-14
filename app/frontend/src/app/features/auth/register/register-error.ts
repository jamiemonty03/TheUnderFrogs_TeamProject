import { HttpErrorResponse } from '@angular/common/http';
import { AUTH_ERRORS, UNREACHABLE_STATUSES } from '../shared/auth-errors';

export const REGISTER_ERRORS = {
  taken: 'That username or email is already taken. If it is yours, log in instead.',
  rejected: "Some details weren't accepted. Check the form and try again.",
  accountNotOpened: "We couldn't open your trading account. Nothing was saved, so try again.",
  ...AUTH_ERRORS,
} as const;

export function registerErrorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return REGISTER_ERRORS.unexpected;
  }
  const errorCode = (error.error as { errorCode?: unknown } | null)?.errorCode;
  const is = (code: string, status: number) =>
    errorCode === code || (errorCode === undefined && error.status === status);

  if (is('AUTH-409', 409)) {
    return REGISTER_ERRORS.taken;
  }
  if (is('VAL-422', 422)) {
    return REGISTER_ERRORS.rejected;
  }
  if (is('AUTH-429', 429)) {
    return REGISTER_ERRORS.tooManyAttempts;
  }
  if (errorCode === 'AUTH-503') {
    return REGISTER_ERRORS.accountNotOpened;
  }
  if (UNREACHABLE_STATUSES.includes(error.status)) {
    return REGISTER_ERRORS.unreachable;
  }
  return REGISTER_ERRORS.unexpected;
}
