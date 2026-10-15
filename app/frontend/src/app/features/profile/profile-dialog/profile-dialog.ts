import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormGroupDirective,
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
import { MatTabsModule } from '@angular/material/tabs';
import { Auth } from '../../../services/auth';
import { Profile, ProfileChanges } from '../../../services/profile';
import { notBlank, strictEmail } from '../../auth/register/register-validators';
import { PROFILE_ERRORS, profileErrorMessage } from '../profile-error';

export const DETAILS_SAVED = 'Your details were saved.';
export const PASSWORD_CHANGED =
  'Your password was changed. Your other devices have been signed out.';

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
  private loaded: ProfileChanges = {};

  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly username = signal('');

  readonly details = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(255), notBlank]],
    email: ['', [Validators.required, Validators.maxLength(255), strictEmail]],
  });
  readonly savingDetails = signal(false);
  readonly detailsMessage = signal<string | null>(null);
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
  readonly passwordMessage = signal<string | null>(null);
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
    this.detailsMessage.set(null);
    this.detailsError.set(null);
    if (Object.keys(changes).length === 0) {
      this.detailsMessage.set(DETAILS_SAVED);
      return;
    }

    this.savingDetails.set(true);
    this.profile.updateProfile(changes).subscribe({
      next: (profile) => {
        this.savingDetails.set(false);
        this.loaded = { fullName: profile.fullName ?? '', email: profile.email };
        this.details.reset({ fullName: this.loaded.fullName, email: this.loaded.email });
        this.detailsMessage.set(DETAILS_SAVED);
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

  changePassword(form: FormGroupDirective): void {
    if (this.password.invalid || this.savingPassword()) {
      return;
    }
    const { currentPassword, newPassword } = this.password.getRawValue();
    this.savingPassword.set(true);
    this.passwordMessage.set(null);
    this.passwordError.set(null);

    this.auth.changePassword(currentPassword, newPassword).subscribe({
      next: () => {
        this.savingPassword.set(false);
        form.resetForm();
        this.showCurrent.set(false);
        this.showNew.set(false);
        this.passwordMessage.set(PASSWORD_CHANGED);
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
}
