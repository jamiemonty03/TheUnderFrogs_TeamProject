import { HttpErrorResponse } from '@angular/common/http';
import { NoTradingAccountError } from '../../services/wallet';
import {
  CONFIRM_ERRORS,
  WALLET_ERRORS,
  canRetryConfirm,
  confirmErrorMessage,
  isPaymentsUnavailable,
  walletErrorMessage,
} from './wallet-error';

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

describe('confirmErrorMessage', () => {
  it.each([
    ['PAY-404', httpError(404, 'PAY-404'), CONFIRM_ERRORS.notFound],
    [
      "PAY-409 uses the server's message",
      httpError(409, 'PAY-409', "This payment hasn't been completed"),
      "This payment hasn't been completed.",
    ],
    [
      "AUTH-403 uses the server's message",
      httpError(403, 'AUTH-403', 'This payment belongs to a different account'),
      'This payment belongs to a different account.',
    ],
    [
      'PAY-503 falls back to the wallet message',
      httpError(503, 'PAY-503'),
      WALLET_ERRORS.unavailable,
    ],
    ['network down', httpError(0), WALLET_ERRORS.unreachable],
  ])('maps %s', (_label, error, message) => {
    expect(confirmErrorMessage(error)).toBe(message);
  });

  it.each([
    ['network down', httpError(0), true],
    ['Stripe failed (PAY-502)', httpError(502, 'PAY-502'), true],
    ['not paid (PAY-409)', httpError(409, 'PAY-409'), false],
    ['not configured (PAY-503)', httpError(503, 'PAY-503'), false],
    ['not found (PAY-404)', httpError(404, 'PAY-404'), false],
  ])('only offers a retry for temporary failures: %s', (_label, error, retry) => {
    expect(canRetryConfirm(error)).toBe(retry);
  });
});
