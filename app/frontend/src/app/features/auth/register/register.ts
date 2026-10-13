import { Component, inject } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { ErrorStateMatcher } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthLayout } from '../auth-layout/auth-layout';
import { SubmittedErrorStateMatcher } from '../login/submitted-error-state';
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
    if (this.form.invalid) {
      return;
    }
  }
}
