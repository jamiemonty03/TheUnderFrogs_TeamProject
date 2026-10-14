import { HttpErrorResponse } from '@angular/common/http';
import { AUTH_ERRORS, UNREACHABLE_STATUSES } from '../shared/auth-errors';

export const LOGIN_ERRORS = {
  invalidCredentials: 'Invalid username or password.',
  ...AUTH_ERRORS,
} as const;

export function loginErrorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return LOGIN_ERRORS.unexpected;
  }
  const errorCode = (error.error as { errorCode?: unknown } | null)?.errorCode;

  if (errorCode === 'AUTH-401' || (errorCode === undefined && error.status === 401)) {
    return LOGIN_ERRORS.invalidCredentials;
  }
  if (errorCode === 'AUTH-429' || (errorCode === undefined && error.status === 429)) {
    return LOGIN_ERRORS.tooManyAttempts;
  }
  if (UNREACHABLE_STATUSES.includes(error.status)) {
    return LOGIN_ERRORS.unreachable;
  }
  return LOGIN_ERRORS.unexpected;
}
