import { HttpErrorResponse } from '@angular/common/http';
import { NoTradingAccountError } from '../../services/wallet';
import { WALLET_ERRORS, isPaymentsUnavailable, walletErrorMessage } from './wallet-error';

const httpError = (status: number, errorCode?: string, message = 'from server') =>
  new HttpErrorResponse({ status, error: errorCode ? { errorCode, message } : null });

describe('walletErrorMessage', () => {
  it.each([
    ['PAY-503 (Stripe not configured)', httpError(503, 'PAY-503'), WALLET_ERRORS.unavailable],
    [
      "VAL-422 shows the server's readable message",
      httpError(422, 'VAL-422', 'The minimum top-up is $10.00'),
      'The minimum top-up is $10.00',
    ],
    ['AUTH-403 (not your account)', httpError(403, 'AUTH-403'), WALLET_ERRORS.notAllowed],
    ['ACC-403 (account suspended)', httpError(403, 'ACC-403'), WALLET_ERRORS.notAllowed],
    ['PAY-502 (Stripe failed)', httpError(502, 'PAY-502'), WALLET_ERRORS.unreachable],
    ['network down', httpError(0), WALLET_ERRORS.unreachable],
    ['a 500', httpError(500, 'SRV-500'), WALLET_ERRORS.unexpected],
    ['no trading account', new NoTradingAccountError(), WALLET_ERRORS.noAccount],
    ['a non-HTTP error', new Error('boom'), WALLET_ERRORS.unexpected],
  ])('maps %s', (_label, error, message) => {
    expect(walletErrorMessage(error)).toBe(message);
  });

  it('only treats PAY-503 as "payments unavailable"', () => {
    expect(isPaymentsUnavailable(httpError(503, 'PAY-503'))).toBe(true);
    expect(isPaymentsUnavailable(httpError(503))).toBe(false);
    expect(isPaymentsUnavailable(new Error('x'))).toBe(false);
  });
});
