import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { Accounts } from '../../../services/accounts';
import { ExternalNavigation } from '../../../services/external-navigation';
import { Wallet } from '../../../services/wallet';
import { isPaymentsUnavailable, walletErrorMessage } from '../wallet-error';

export const MIN_TOP_UP = 10;
export const MAX_TOP_UP = 10000;

function atMostTwoDecimals(control: AbstractControl<number | null>): ValidationErrors | null {
  const value = control.value;
  return value === null || /^\d+(\.\d{1,2})?$/.test(String(value)) ? null : { decimals: true };
}

@Component({
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  selector: 'app-wallet-dialog',
  styleUrl: './wallet-dialog.scss',
  templateUrl: './wallet-dialog.html',
})
export class WalletDialog {
  private readonly accounts = inject(Accounts);
  private readonly wallet = inject(Wallet);
  private readonly router = inject(Router);
  private readonly navigation = inject(ExternalNavigation);
  private readonly dialogRef = inject(MatDialogRef<WalletDialog>);

  readonly quickAmounts = [100, 500, 1000];
  readonly balance = signal<number | null>(null);
  readonly balanceLoading = this.accounts.balance.loading;
  readonly submitting = signal(false);
  readonly unavailable = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = new FormGroup({
    amount: new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(MIN_TOP_UP),
      Validators.max(MAX_TOP_UP),
      atMostTwoDecimals,
    ]),
  });

  constructor() {
    this.accounts.getBalance().subscribe({
      next: (balance) => this.balance.set(balance),
      error: () => this.balance.set(null),
    });
  }

  pick(amount: number): void {
    this.form.controls.amount.setValue(amount);
  }

  submit(): void {
    const amount = this.form.controls.amount.value;
    if (this.form.invalid || amount === null || this.submitting() || this.unavailable()) {
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);

    this.wallet.startTopUp(amount, this.router.url).subscribe({
      next: (checkoutUrl) => this.navigation.goTo(checkoutUrl),
      error: (error: unknown) => {
        this.submitting.set(false);
        this.unavailable.set(isPaymentsUnavailable(error));
        this.errorMessage.set(walletErrorMessage(error));
      },
    });
  }

  close(): void {
    this.dialogRef.close();
  }
}
