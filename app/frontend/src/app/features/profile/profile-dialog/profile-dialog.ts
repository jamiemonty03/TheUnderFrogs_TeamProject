import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { Auth } from '../../../services/auth';
import { Profile, ProfileChanges } from '../../../services/profile';
import { notBlank, strictEmail } from '../../auth/register/register-validators';
import { PROFILE_ERRORS, profileErrorMessage } from '../profile-error';

export const DETAILS_SAVED = 'Your details were saved.';
export const PASSWORD_CHANGED =
  'Your password was changed. Your other devices have been signed out.';
const NOTICE_DURATION_MS = 6000;

function matchingPasswords(group: AbstractControl): ValidationErrors | null {
  const { newPassword, confirmPassword } = group.value as Record<string, string>;
  return confirmPassword && newPassword !== confirmPassword ? { mismatch: true } : null;
}

@Component({
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTabsModule,
  ],
  selector: 'app-profile-dialog',
  styleUrl: './profile-dialog.scss',
  templateUrl: './profile-dialog.html',
})
export class ProfileDialog {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly profile = inject(Profile);
  private readonly auth = inject(Auth);
  private readonly dialogRef = inject(MatDialogRef<ProfileDialog>);
  private readonly snackBar = inject(MatSnackBar);
  private loaded: ProfileChanges = {};

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly username = signal('');

  readonly details = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(255), notBlank]],
    email: ['', [Validators.required, Validators.maxLength(255), strictEmail]],
  });
  readonly savingDetails = signal(false);
  readonly detailsError = signal<string | null>(null);

  readonly password = this.fb.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: [
        '',
        [Validators.required, Validators.minLength(12), Validators.maxLength(128), notBlank],
      ],
      confirmPassword: ['', Validators.required],
    },
    { validators: matchingPasswords },
  );
  readonly savingPassword = signal(false);
  readonly passwordError = signal<string | null>(null);
  readonly showCurrent = signal(false);
  readonly showNew = signal(false);

  constructor() {
    this.profile.getProfile().subscribe({
      next: (profile) => {
        this.username.set(profile.username);
        this.loaded = { fullName: profile.fullName ?? '', email: profile.email };
        this.details.reset({ fullName: this.loaded.fullName, email: this.loaded.email });
        this.loading.set(false);
      },
      error: () => {
        this.loadError.set(PROFILE_ERRORS.loadFailed);
        this.loading.set(false);
      },
    });
  }

  saveDetails(): void {
    if (this.details.invalid || this.savingDetails()) {
      return;
    }
    const { fullName, email } = this.details.getRawValue();
    const changes: ProfileChanges = {
      ...(fullName.trim() !== this.loaded.fullName ? { fullName: fullName.trim() } : {}),
      ...(email.trim() !== this.loaded.email ? { email: email.trim() } : {}),
    };
    this.detailsError.set(null);
    if (Object.keys(changes).length === 0) {
      this.closeWith(DETAILS_SAVED);
      return;
    }

    this.savingDetails.set(true);
    this.profile.updateProfile(changes).subscribe({
      next: () => {
        this.savingDetails.set(false);
        this.closeWith(DETAILS_SAVED);
      },
      error: (error: unknown) => {
        this.savingDetails.set(false);
        if (error instanceof HttpErrorResponse && error.status === 409) {
          this.details.controls.email.setErrors({ taken: profileErrorMessage(error) });
          return;
        }
        this.detailsError.set(profileErrorMessage(error));
      },
    });
  }

  changePassword(): void {
    if (this.password.invalid || this.savingPassword()) {
      return;
    }
    const { currentPassword, newPassword } = this.password.getRawValue();
    this.savingPassword.set(true);
    this.passwordError.set(null);

    this.auth.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.savingPassword.set(false);
        this.closeWith(PASSWORD_CHANGED);
      },
      error: (error: unknown) => {
        this.savingPassword.set(false);
        if (error instanceof HttpErrorResponse && error.status === 403) {
          this.password.controls.currentPassword.setErrors({
            incorrect: profileErrorMessage(error),
          });
          return;
        }
        this.passwordError.set(profileErrorMessage(error));
      },
    });
  }

  close(): void {
    this.dialogRef.close();
  }

  private closeWith(message: string): void {
    this.dialogRef.close();
    this.snackBar.open(message, 'OK', { duration: NOTICE_DURATION_MS });
  }
}
