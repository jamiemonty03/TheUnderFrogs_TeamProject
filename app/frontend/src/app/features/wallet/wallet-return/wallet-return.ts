import { formatCurrency } from '@angular/common';
import { Component, LOCALE_ID, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { Wallet } from '../../../services/wallet';
import { DEFAULT_AFTER_LOGIN, safeReturnUrl } from '../../auth/login/return-url';
import { canRetryConfirm, confirmErrorMessage } from '../wallet-error';

export const CANCELLED_NOTICE = 'Payment cancelled. No money was taken.';
const NOTICE_DURATION_MS = 6000;

interface ConfirmFailure {
  message: string;
  canRetry: boolean;
}

export function safeWalletReturn(returnTo: string | null): string {
  const safe = safeReturnUrl(returnTo);
  return safe.startsWith('/wallet') ? DEFAULT_AFTER_LOGIN : safe;
}

@Component({
  imports: [MatButtonModule, MatProgressSpinnerModule],
  selector: 'app-wallet-return',
  styleUrl: './wallet-return.scss',
  templateUrl: './wallet-return.html',
})
export class WalletReturn {
  private readonly router = inject(Router);
  private readonly wallet = inject(Wallet);
  private readonly snackBar = inject(MatSnackBar);
  private readonly locale = inject(LOCALE_ID);
  private readonly params = inject(ActivatedRoute).snapshot.queryParamMap;
  private readonly sessionId = this.params.get('session_id');

  readonly returnTo = safeWalletReturn(this.params.get('returnTo'));
  readonly failure = signal<ConfirmFailure | null>(null);

  constructor() {
    if (this.params.get('cancelled') === '1') {
      this.leave(CANCELLED_NOTICE);
    } else if (!this.sessionId) {
      this.back();
    } else {
      this.confirm();
    }
  }

  confirm(): void {
    if (!this.sessionId) {
      return;
    }
    this.failure.set(null);
    this.wallet.confirmTopUp(this.sessionId).subscribe({
      next: ({ amount }) =>
        this.leave(
          `Added ${formatCurrency(amount, this.locale, '$', 'USD')} to your cash balance.`,
        ),
      error: (error: unknown) =>
        this.failure.set({
          message: confirmErrorMessage(error),
          canRetry: canRetryConfirm(error),
        }),
    });
  }

  back(): void {
    void this.router.navigateByUrl(this.returnTo, { replaceUrl: true });
  }

  private leave(message: string): void {
    void this.router
      .navigateByUrl(this.returnTo, { replaceUrl: true })
      .then(() => this.snackBar.open(message, 'OK', { duration: NOTICE_DURATION_MS }));
  }
}
