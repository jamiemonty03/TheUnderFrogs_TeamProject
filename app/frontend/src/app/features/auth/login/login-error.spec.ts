import { HttpErrorResponse } from '@angular/common/http';
import { LOGIN_ERRORS, loginErrorMessage } from './login-error';

const httpError = (status: number, error: unknown = null) =>
  new HttpErrorResponse({ status, error });

describe('loginErrorMessage', () => {
  it.each([
    [
      'AUTH-401 from auth-service',
      httpError(401, { errorCode: 'AUTH-401', message: 'Invalid credentials' }),
      LOGIN_ERRORS.invalidCredentials,
    ],
    ['a 401 with no errorCode', httpError(401), LOGIN_ERRORS.invalidCredentials],
    [
      'AUTH-429 from auth-service',
      httpError(429, { errorCode: 'AUTH-429', message: 'Too many attempts' }),
      LOGIN_ERRORS.tooManyAttempts,
    ],
    ['a 429 with no errorCode', httpError(429), LOGIN_ERRORS.tooManyAttempts],
    ['no response at all (network failure)', httpError(0), LOGIN_ERRORS.unreachable],
    ['502 Bad Gateway', httpError(502), LOGIN_ERRORS.unreachable],
    ['503 Service Unavailable', httpError(503), LOGIN_ERRORS.unreachable],
    ['504 from the dev proxy when auth-service is down', httpError(504), LOGIN_ERRORS.unreachable],
    [
      'a 500',
      httpError(500, { errorCode: 'SRV-500', message: 'Unexpected error' }),
      LOGIN_ERRORS.unexpected,
    ],
    [
      'a 422 validation error',
      httpError(422, { errorCode: 'VAL-422', message: 'username must not be blank' }),
      LOGIN_ERRORS.unexpected,
    ],
    ['something that is not an HTTP error', new Error('boom'), LOGIN_ERRORS.unexpected],
  ])('%s', (_label, error, message) => {
    expect(loginErrorMessage(error)).toBe(message);
  });

  it('uses the exact wording from the ticket', () => {
    expect(LOGIN_ERRORS.invalidCredentials).toBe('Invalid username or password.');
    expect(LOGIN_ERRORS.tooManyAttempts).toBe('Too many attempts. Wait a minute and try again.');
    expect(LOGIN_ERRORS.unreachable).toBe("Can't reach the sign-in service.");
  });
});
