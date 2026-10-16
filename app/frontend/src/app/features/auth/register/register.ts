import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { Auth } from '../../../services/auth';
import { AuthLayout } from '../auth-layout/auth-layout';
import { SubmittedErrorStateMatcher } from '../shared/submitted-error-state';
import { registerErrorMessage } from './register-error';
import { USERNAME_PATTERN, notBlank, strictEmail } from './register-validators';

@Component({
  imports: [ReactiveFormsModule, AuthLayout, MatFormFieldModule, MatInputModule, MatButtonModule],
  providers: [{ provide: ErrorStateMatcher, useClass: SubmittedErrorStateMatcher }],
  selector: 'app-register',
  styleUrl: './register.scss',
  templateUrl: './register.html',
})
export class Register {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  readonly submitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(255), notBlank]],
    username: [
      '',
      [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(50),
        Validators.pattern(USERNAME_PATTERN),
      ],
    ],
    email: ['', [Validators.required, strictEmail]],
    password: [
      '',
      [Validators.required, Validators.minLength(12), Validators.maxLength(128), notBlank],
    ],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    const { fullName, username, email, password } = this.form.getRawValue();

    this.auth
      .register({ fullName: fullName.trim(), username, email: email.trim(), password })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/dashboard'),
        error: (error: unknown) => this.errorMessage.set(registerErrorMessage(error)),
      });
  }
}
