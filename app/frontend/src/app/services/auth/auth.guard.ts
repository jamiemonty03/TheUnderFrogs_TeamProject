import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from './auth';

const HOME_URL = '/dashboard';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(Auth);
  if (auth.isAuthenticated()) {
    return true;
  }
  auth.clearSession();
  const queryParams = state.url === HOME_URL ? {} : { returnUrl: state.url };
  return inject(Router).createUrlTree(['/login'], { queryParams });
};
