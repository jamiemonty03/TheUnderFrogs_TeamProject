import { HttpErrorResponse } from '@angular/common/http';
import { AUTH_ERRORS, UNREACHABLE_STATUSES } from '../auth/shared/auth-errors';

export const PROFILE_ERRORS = {
  loadFailed: "Couldn't load your profile. Try again in a moment.",
  ...AUTH_ERRORS,
} as const;

function serverMessage(error: HttpErrorResponse): string | null {
  const message = (error.error as { message?: unknown } | null)?.message;
  if (typeof message !== 'string' || !message.trim()) {
    return null;
  }
  return message.endsWith('.') ? message : `${message}.`;
}

export function profileErrorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return PROFILE_ERRORS.unexpected;
  }
  if (error.status === 429) {
    return PROFILE_ERRORS.tooManyAttempts;
  }
  if ([403, 409, 422].includes(error.status)) {
    return serverMessage(error) ?? PROFILE_ERRORS.unexpected;
  }
  if (UNREACHABLE_STATUSES.includes(error.status)) {
    return PROFILE_ERRORS.unreachable;
  }
  return PROFILE_ERRORS.unexpected;
}
