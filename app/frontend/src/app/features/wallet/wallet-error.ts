import { HttpErrorResponse } from '@angular/common/http';
import { NoTradingAccountError } from '../../services/wallet';

export const WALLET_ERRORS = {
  unavailable: "Adding funds isn't available right now.",
  noAccount: 'No trading account is linked to this user.',
  notAllowed: "This account can't be topped up.",
  unreachable: "Can't reach the payment service. Try again in a moment.",
  unexpected: 'Something went wrong. Try again.',
} as const;

const UNREACHABLE_STATUSES = [0, 502, 504];

export function errorCodeOf(error: unknown): unknown {
  return error instanceof HttpErrorResponse
    ? (error.error as { errorCode?: unknown } | null)?.errorCode
    : undefined;
}

export function isPaymentsUnavailable(error: unknown): boolean {
  return errorCodeOf(error) === 'PAY-503';
}

export function walletErrorMessage(error: unknown): string {
  if (error instanceof NoTradingAccountError) {
    return WALLET_ERRORS.noAccount;
  }
  if (!(error instanceof HttpErrorResponse)) {
    return WALLET_ERRORS.unexpected;
  }
  const errorCode = errorCodeOf(error);
  const serverMessage = (error.error as { message?: unknown } | null)?.message;

  if (errorCode === 'PAY-503') {
    return WALLET_ERRORS.unavailable;
  }
  if (errorCode === 'VAL-422' && typeof serverMessage === 'string') {
    return serverMessage;
  }
  if (errorCode === 'AUTH-403' || errorCode === 'ACC-403') {
    return WALLET_ERRORS.notAllowed;
  }
  if (errorCode === 'PAY-502' || UNREACHABLE_STATUSES.includes(error.status)) {
    return WALLET_ERRORS.unreachable;
  }
  return WALLET_ERRORS.unexpected;
}
