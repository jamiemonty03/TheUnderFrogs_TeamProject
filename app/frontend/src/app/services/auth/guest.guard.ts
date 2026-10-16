import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from './auth';

export const guestGuard: CanActivateFn = () => {
  const auth = inject(Auth);
  return auth.isAuthenticated() ? inject(Router).createUrlTree(['/dashboard']) : true;
};
