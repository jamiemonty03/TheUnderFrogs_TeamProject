import { HttpErrorResponse } from '@angular/common/http';

export const LOGIN_ERRORS = {
  invalidCredentials: 'Invalid username or password.',
  tooManyAttempts: 'Too many attempts. Wait a minute and try again.',
  unreachable: "Can't reach the sign-in service.",
  unexpected: 'Something went wrong. Try again.',
} as const;

const UNREACHABLE_STATUSES = [0, 502, 503, 504];

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
