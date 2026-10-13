import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { Auth } from '../../../services/auth';
import { loginErrorMessage } from './login-error';
import { safeReturnUrl } from './return-url';
import { SubmittedErrorStateMatcher } from './submitted-error-state';

@Component({
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule],
  providers: [{ provide: ErrorStateMatcher, useClass: SubmittedErrorStateMatcher }],
  selector: 'app-login',
  styleUrl: './login.scss',
  templateUrl: './login.html',
})
export class Login {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly tickerQuotes = [
    { symbol: 'AAPL', change: 1.24 },
    { symbol: 'MSFT', change: 0.88 },
    { symbol: 'TSLA', change: -2.1 },
    { symbol: 'SPY', change: 0.41 },
    { symbol: 'NVDA', change: 3.02 },
    { symbol: 'BND', change: -0.12 },
  ].map(({ symbol, change }) => ({
    symbol,
    up: change >= 0,
    percent: Math.abs(change).toFixed(2),
  }));

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const { username, password } = this.form.getRawValue();

    this.auth
      .login(username, password)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
          void this.router.navigateByUrl(safeReturnUrl(returnUrl));
        },
        error: (error: unknown) => this.errorMessage.set(loginErrorMessage(error)),
      });
  }
}
