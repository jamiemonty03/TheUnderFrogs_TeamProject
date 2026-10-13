import { HttpErrorResponse } from '@angular/common/http';
import { REGISTER_ERRORS, registerErrorMessage } from './register-error';

const httpError = (status: number, errorCode?: string) =>
  new HttpErrorResponse({
    status,
    error: errorCode
      ? { errorCode, message: 'User with email kermit@pond.io already exists' }
      : null,
  });

describe('registerErrorMessage', () => {
  it.each([
    ['AUTH-409 (username or email taken)', httpError(409, 'AUTH-409'), REGISTER_ERRORS.taken],
    ['a bare 409', httpError(409), REGISTER_ERRORS.taken],
    ['VAL-422 (a field was rejected)', httpError(422, 'VAL-422'), REGISTER_ERRORS.rejected],
    ['a bare 422', httpError(422), REGISTER_ERRORS.rejected],
    ['AUTH-429 (rate limited)', httpError(429, 'AUTH-429'), REGISTER_ERRORS.tooManyAttempts],
    [
      'AUTH-503 (accounts service could not open the account)',
      httpError(503, 'AUTH-503'),
      REGISTER_ERRORS.accountNotOpened,
    ],
    ['a bare 503 from the proxy', httpError(503), REGISTER_ERRORS.unreachable],
    ['status 0 (network down)', httpError(0), REGISTER_ERRORS.unreachable],
    ['a 502 bad gateway', httpError(502), REGISTER_ERRORS.unreachable],
    ['a 504 gateway timeout', httpError(504), REGISTER_ERRORS.unreachable],
    ['a 500 server error', httpError(500, 'SRV-500'), REGISTER_ERRORS.unexpected],
    ['a non-HTTP error', new Error('boom'), REGISTER_ERRORS.unexpected],
  ])('maps %s to the right message', (_label, error, message) => {
    expect(registerErrorMessage(error)).toBe(message);
  });

  it('trusts the errorCode over the status', () => {
    expect(registerErrorMessage(httpError(400, 'AUTH-409'))).toBe(REGISTER_ERRORS.taken);
  });

  it('never repeats the server message, so it cannot leak which detail was taken', () => {
    const message = registerErrorMessage(httpError(409, 'AUTH-409'));

    expect(message).not.toContain('kermit@pond.io');
    expect(message).not.toContain('email already exists');
  });
});
