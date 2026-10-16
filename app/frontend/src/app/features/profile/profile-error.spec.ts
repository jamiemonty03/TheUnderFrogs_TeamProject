import { HttpErrorResponse } from '@angular/common/http';
import { PROFILE_ERRORS, profileErrorMessage } from './profile-error';

const httpError = (status: number, message?: string) =>
  new HttpErrorResponse({ status, error: message ? { errorCode: 'X', message } : null });

describe('profileErrorMessage', () => {
  it.each([
    [
      'a taken email (409)',
      httpError(409, 'That email is already used by another account'),
      'That email is already used by another account.',
    ],
    [
      'a wrong current password (403)',
      httpError(403, 'Your current password is incorrect'),
      'Your current password is incorrect.',
    ],
    [
      'a rejected value (422)',
      httpError(422, 'Choose a new password that is different from your current one'),
      'Choose a new password that is different from your current one.',
    ],
    ['a message that already ends with a full stop', httpError(409, 'Try again.'), 'Try again.'],
    ['too many attempts (429)', httpError(429), PROFILE_ERRORS.tooManyAttempts],
    ['the service being unreachable', httpError(0), PROFILE_ERRORS.unreachable],
    ['a 409 with no message', httpError(409), PROFILE_ERRORS.unexpected],
    ['a 500', httpError(500, 'Unexpected error'), PROFILE_ERRORS.unexpected],
    ['a non-HTTP error', new Error('boom'), PROFILE_ERRORS.unexpected],
  ])('maps %s', (_label, error, message) => {
    expect(profileErrorMessage(error)).toBe(message);
  });
});
